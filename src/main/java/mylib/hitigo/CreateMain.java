package mylib.hitigo;

import com.meterware.httpunit.ClientProperties;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HTMLElement;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class CreateMain
{
  public static final String USERAGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0";
  public static String lastPage = null;

  public static void main( String args[] )
  {
    try {
      final String server = args[0];
      final String loginid = args[1];
      final String password = args[2];
      final String folder = args[3];
      final String TIMESTAMP = "19950101"+"000000";

      // FTPClientの生成
      FTPClient ftpclient = new FTPClient();

      // FTPサーバに接続
      System.out.println("ftp connect : "+server);
      ftpclient.connect(server);
      int reply = ftpclient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
	throw new Exception("connection failure : "+server+" : "+ftpclient.getReplyString());
      }

      // ログイン
      System.out.println("ftp login : "+loginid);
      if (ftpclient.login(loginid,password) == false) {
	throw new Exception("login failure : "+loginid+" : "+ftpclient.getReplyString());
      }

      // バイナリモードに設定
      ftpclient.setFileType(FTP.BINARY_FILE_TYPE);

      // タイムスタンプ修正
      ftpclient.setModificationTime(folder+"/igo-top.html",TIMESTAMP);

      for ( int year = 1996 /*2007*/; year <= 2015 /*2008*/; ++year ) {
	// クロール＆ファイル生成
	byte data[][] = createPage1(year);

	// ファイル出力
	String filename;
	filename = folder+"/igo-"+year+"a.html";
	if ( !ftpclient.storeFile(filename,new ByteArrayInputStream(data[0])) ) {
	  throw new IOException("store file failure : "+filename);
	}
	ftpclient.setModificationTime(filename,TIMESTAMP);

	filename = folder+"/igo-"+year+"b.html";
	if ( !ftpclient.storeFile(filename,new ByteArrayInputStream(data[1])) ) {
	  throw new IOException("store file failure : "+filename);
	}
	ftpclient.setModificationTime(filename,TIMESTAMP);
      }
      // 終了処理
      ftpclient.logout();
      ftpclient.disconnect();
    } catch ( Exception ex ) {
      System.out.println("-- response -- start");
      System.out.println(lastPage);
      System.out.println("-- response -- end");
      ex.printStackTrace();
    }
  }

  public static byte[][] createPage1( int year )
  throws IOException, SAXException
  {
    HttpUnitOptions.setScriptingEnabled(false);
    HttpUnitOptions.setDefaultCharacterSet("Shift_JIS");
    ClientProperties.getDefaultProperties().setUserAgent(USERAGENT);

    ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
    ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
    PrintStream out1 = new PrintStream(bout1);
    PrintStream out2 = new PrintStream(bout2);

    WebConversation wc = new WebConversation();
    WebRequest request;
    WebResponse response;

    request = new GetMethodWebRequest("http://www.hitachi.co.jp/Sp/tsumego/past/"+year+".html");
    response = wc.getResponse(request);
    lastPage = response.getText();

    Enumeration<WebLink[]> linkenum = new MyEnum(response);
    out1.println("<HTML>");
    out1.println("<TITLE>"+year+"年 初級</TITLE>");
    out1.println("<HEAD>");
    out1.println("<meta name=viewport content=\"width=300\">");
    out1.println("</HEAD>");
    out1.println("<BODY>");
    out2.println("<HTML>");
    out2.println("<TITLE>"+year+"年 中級</TITLE>");
    out2.println("<HEAD>");
    out2.println("<meta name=viewport content=\"width=300\">");
    out2.println("</HEAD>");
    out2.println("<BODY>");
    while ( linkenum.hasMoreElements() ) {
      out1.println("<hr size=5 noshade>");
      out2.println("<hr size=5 noshade>");
      for ( WebLink link : linkenum.nextElement() ) {
	request = link.getRequest();
	URL url = request.getURL();
	response = wc.getResponse(request);
	lastPage = response.getText();
	HTMLElement elems[] = response.getElementsByTagName("STRONG");
	if ( elems.length == 0 ) {
	  // タイトル
	  elems = response.getElementsByTagName("H1");
	  String text = textToTag(elems[0].getNode(),"A");
	  out1.println("<strong>"+text+"</strong>");
	  out2.println("<strong>"+text+"</strong>");

	  // 中身
	  elems = response.getElementsByTagName("H2");
	  Node node = elems[0].getNode();
	  PrintStream out = out1;
	  for ( int x = 0; node != null; ++x ) {
	    System.out.println("[B] "+link.getAttribute("indexstr")+", "+x);
	    StringBuffer strbuf = new StringBuffer();
	    node = traverseToTag(node,"IMG",strbuf,null);
	    if ( strbuf.indexOf("中級") >= 0 ) out = out2;
	    String src = node.getAttributes().getNamedItem("src").getNodeValue();
	    StringBuffer comment = new StringBuffer();
	    node = traverseToTag(nextNode(node),new String[]{"DIV","SPAN","A"},comment,url);
	    //node = traverseToTag(nextNode(node),"DIV",strbuf,null);
	    node = traverseToTag(node,new String[]{"H2","H3"},strbuf,null);
	    int idx = strbuf.indexOf("ページトップ");
	    if ( idx > 0 ) strbuf.delete(idx,strbuf.length());
	    out.println("<p>"+strbuf+"</p>");
	    out.println("<p><img width=280 src=\""+new URL(url,src)+"\"></p>");
	    out.println("<p>"+comment+"</p>");
	  }
	  continue;
	}
	String title = response.getElementsByTagName("H1")[0].getText();
	out1.println("<strong>"+title+" "+elems[0].getText()+"</strong>");
	out2.println("<strong>"+title+" "+elems[0].getText()+"</strong>");
	PrintStream out = out1;
	for ( int x = 1; x < elems.length; ++x ) {
	  System.out.println("[A] "+link.getAttribute("indexstr")+", "+x);
	  Node node = elems[x].getNode();
	  StringBuffer strbuf = new StringBuffer();
	  node = traverseToTag(node,"IMG",strbuf,url);
	  String src = node.getAttributes().getNamedItem("src").getNodeValue();
	  if ( strbuf.indexOf("中級") > 0 ) out = out2;
	  out.println("<p>"+strbuf+"</p>");
	  out.println("<p><img width=280 src=\""+new URL(url,src)+"\"></p>");
	  strbuf = new StringBuffer();
	  node = traverseToTag(nextNode(node),new String[]{"STRONG","FONT","A"},strbuf,url);
	  out.println("<p>"+strbuf+"</p>");
	}
      }
    }
    out1.println("</BODY>");
    out1.println("</HTML>");
    out2.println("</BODY>");
    out2.println("</HTML>");
    out1.close();
    out2.close();
    return new byte[][]{
      bout1.toByteArray(),
      bout2.toByteArray(),
    };
  }

  public static class MyEnum implements Enumeration<WebLink[]>
  {
    public WebTable table;
    public int rows;
    public int cols;
    public int idxi;
    public int idxj;

    public WebLink links[];
    public int idx;

    public MyEnum( WebResponse response )
    throws SAXException
    {
      WebTable tables[] = response.getTables();
      if ( tables.length >= 2 ) {
	table = tables[2].getTableCell(0,0).getTables()[2];
	idxi = 0;
	idxj = 0;
	rows = table.getRowCount();
	cols = table.getColumnCount();
      } else {
	table = null;
	links = response.getLinks();
	idx = 0;
	nextTag("問題");
      }
    }

    public boolean hasMoreElements()
    {
      if ( table != null ) {
	return (idxi < rows) && (idxj < cols) && (table.getTableCell(idxi,idxj).getLinkWith("問題") != null);
      } else {
	return (idx < links.length);
      }
    }

    public WebLink[] nextElement()
    {
      WebLink ret[] = new WebLink[2];
      if ( table != null ) {
	TableCell cell = table.getTableCell(idxi,idxj);
	for ( int i = 0; i < ret.length; ++i ) {
	  String type = new String[]{ "問題", "答え" }[i];
	  ret[i] = cell.getLinkWith(type);
	  ret[i].setAttribute("indexstr",""+idxi+", "+idxj+", "+type);
	}
	if ( ++idxj >= cols ) {
	  idxj = 0;
	  ++idxi;
	}
      } else {
	ret[0] = links[idx];
	ret[0].setAttribute("indexstr",""+idx+", 問題");
	++idx;
	nextTag("答え");
	ret[1] = links[idx];
	ret[1].setAttribute("indexstr",""+idx+", 答え");
	++idx;
	nextTag("問題");
      }
      return ret;
    }

    public void nextTag( String text )
    {
      for ( ; idx < links.length; ++idx ) {
	String str = links[idx].getText();
	if ( str.indexOf("過去") >= 0 ) continue;
	if ( str.indexOf(text) >= 0 ) break;
      }
    }
  }

  public static String textToTag( Node node, String tagName )
  throws MalformedURLException
  {
    StringBuffer strbuf = new StringBuffer();
    traverseToTag(node,tagName,strbuf,null);
    return strbuf.toString();
  }

  public static Node traverseToTag( Node node, String tagName, StringBuffer strbuf, URL url )
  throws MalformedURLException
  {
    return traverseToTag(node,new String[]{tagName},strbuf,url);
  }

  public static Node traverseToTag( Node node, String tagNames[], StringBuffer strbuf, URL url )
  throws MalformedURLException
  {
    while ( node != null ) {
      String nodeName = node.getNodeName();
      for ( String tagName : tagNames ) {
        if ( nodeName.equals(tagName) ) return node;
      }
      if ( node instanceof Text ) strbuf.append(((Text)node).getData().replace('\u00a0',' '));
      if ( url != null && nodeName.equals("IMG") ) {
	String src = node.getAttributes().getNamedItem("src").getNodeValue();
	strbuf.append("<IMG src=\"").append(new URL(url,src)).append("\">");
      }
      node = nextNode(node);
    }
    return node;
  }

  public static Node nextNode( Node node )
  {
    Node next = node.getFirstChild();
    if ( next == null ) {
      while ( node != null && (next = node.getNextSibling()) == null ) {
	node = node.getParentNode();
      }
    }
    return next;
  }
}
