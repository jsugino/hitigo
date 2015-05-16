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
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class CreateMain
{
  public static String USERAGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0";
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

      for ( int year = 1998; year <= 1998; ++year ) {
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

    WebTable table = response.getTables()[2];
    table = table.getTableCell(0,0).getTables()[2];
    int rows = table.getRowCount();
    int cols = table.getColumnCount();
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
    for ( int i = 0; i < rows; ++i ) {
      for ( int j = 0; j < cols; ++j ) {
	TableCell cell = table.getTableCell(i,j);
	out1.println("<hr size=5 noshade>");
	out2.println("<hr size=5 noshade>");
	for ( String type : new String[] { "問題", "答え" } ) {
	  WebLink link = cell.getLinkWith(type);
	  if ( link == null ) break;
	  request = link.getRequest();
	  URL url = request.getURL();
	  response = wc.getResponse(request);
	  lastPage = response.getText();
	  HTMLElement elems[] = response.getElementsByTagName("STRONG");
	  out1.println("<strong>"+type+" "+elems[0].getText()+"</strong>");
	  out2.println("<strong>"+type+" "+elems[0].getText()+"</strong>");
	  PrintStream out = out1;
	  for ( int x = 1; x < elems.length; ++x ) {
	    System.out.println("page "+i+", "+j+", "+x);
	    Node node = elems[x].getNode();
	    StringBuffer strbuf = new StringBuffer();
	    node = traverseToTag(node,"IMG",strbuf,url);
	    String src = node.getAttributes().getNamedItem("src").getNodeValue();
	    if ( strbuf.indexOf("中級") > 0 ) out = out2;
	    out.println("<p>"+strbuf+"</p>");
	    out.println("<p><img width=280 src=\""+new URL(url,src)+"\"></p>");
	    strbuf = new StringBuffer();
	    node = traverseToTag(nextNode(node),"STRONG",strbuf,url);
	    out.println("<p>"+strbuf+"</p>");
	  }
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

  public static Node traverseToTag( Node node, String tagName, StringBuffer strbuf, URL url )
  throws MalformedURLException
  {
    while ( node != null && !node.getNodeName().equals(tagName) ) {
      if ( node instanceof Text ) strbuf.append(((Text)node).getData());
      if ( node.getNodeName().equals("IMG") ) {
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
