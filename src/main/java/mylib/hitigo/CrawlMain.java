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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
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

public class CrawlMain
{
  public static final String TIMESTAMP = "19950101"+"000000";
  public static final String USERAGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0";
  public static String lastPage = null;

  public static void main( String args[] )
  {
    try {
      for ( int year = 1996 /*2010*/; year <= 2015 /*2010*/; ++year ) {
	//crawlPage(year);
      }
    } catch ( Exception ex ) {
      System.out.println("-- response -- start");
      System.out.println(lastPage);
      System.out.println("-- response -- end");
      ex.printStackTrace();
    }

    String data[] = new String[30000];
    int counts[] = new int[3000];
    int probnum = 0;
    int total = 0;
    try {
      for ( int year = 1996; year <= 2015; ++year ) {
	BufferedReader in = new BufferedReader(new FileReader("target/dump/igo"+year+".txt"));
	String line;
	int mode = 0;
	while ( (line = in.readLine()) != null ) {
	  switch ( mode ) {
	   case 0:
	    if ( line.startsWith("####") ) {
	      line = line.substring(4);
	      ++mode;
	      counts[probnum++] = total;
	    }
	    break;
	   case 1:
	    if ( line.startsWith("####") ) {
	      line = line.substring(4);
	      ++mode;
	      if ( (total - counts[probnum-1]) != 7 ) {
		throw new Exception("ERROR number "+probnum+" "+total);
	      }
	    }
	    break;
	   case 2:
	    if ( line.indexOf("中級") >= 0 ) {
	      counts[probnum++] = total;
	      mode = 0;
	    }
	    break;
	  }
	  data[total++] = line;
	}
	in.close();
      }
      counts[probnum] = total;

      // パラメータ
      final String server = args[0];
      final String loginid = args[1];
      final String password = args[2];
      final String folder = args[3];

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

      /*
      for ( int i = 0; i < probnum; ++i ) {
	System.out.println(""+i+" "+(counts[i+1]-counts[i])+" "+data[counts[i]]);
      }
      */
      for ( int i = 0; i < probnum; i += 20 ) {
	// ページ生成
	byte pages[][] = createPage(data,counts,i,Math.min(probnum,i+20));

	// ファイル出力
	String filename;
	filename = folder+"/igo-"+(i/2)+"-a.html";
	if ( !ftpclient.storeFile(filename,new ByteArrayInputStream(pages[0])) ) {
	  throw new IOException("store file failure : "+filename);
	}
	ftpclient.setModificationTime(filename,TIMESTAMP);

	filename = folder+"/igo-"+(i/2)+"-b.html";
	if ( !ftpclient.storeFile(filename,new ByteArrayInputStream(pages[1])) ) {
	  throw new IOException("store file failure : "+filename);
	}
	ftpclient.setModificationTime(filename,TIMESTAMP);
      }

      // 終了処理
      ftpclient.logout();
      ftpclient.disconnect();
    } catch ( Exception ex ) {
      ex.printStackTrace();
    }
  }

  public static void crawlPage( int year )
  throws IOException, SAXException
  {
    HttpUnitOptions.setScriptingEnabled(false);
    HttpUnitOptions.setDefaultCharacterSet("Shift_JIS");
    ClientProperties.getDefaultProperties().setUserAgent(USERAGENT);

    WebConversation wc = new WebConversation();
    WebRequest request;
    WebResponse response;

    new File("target/dump").mkdirs();
    PrintStream dumpout = new PrintStream("target/dump/igo"+year+".txt");

    request = new GetMethodWebRequest("http://www.hitachi.co.jp/Sp/tsumego/past/"+year+".html");
    response = wc.getResponse(request);
    lastPage = response.getText();

    Enumeration<WebLink[]> linkenum = new MyEnum(response);
    while ( linkenum.hasMoreElements() ) {
      for ( WebLink link : linkenum.nextElement() ) {
	request = link.getRequest();
	URL url = request.getURL();
	response = wc.getResponse(request);
	lastPage = response.getText();
	HTMLElement elems[] = response.getElementsByTagName("H2");
	String title = response.getElementsByTagName("H1")[0].getText();
	if ( elems.length > 0 ) {
	  int probnum = Integer.parseInt(title.replaceAll("[^0-9]",""));
	  printTitle(dumpout,title);
	  Node node = elems[0].getNode();
	  for ( int x = 0; node != null; ++x ) {
	    StringBuffer strbuf = new StringBuffer();
	    node = traverseToTag(node,new String[]{"IMG"},strbuf,null);
	    if ( strbuf.indexOf("免責事項") >= 0 ) {
	      node = traverseToTag(node,new String[]{"H2","H3"},strbuf,null);
	      continue;
	    }
	    String src = node.getAttributes().getNamedItem("src").getNodeValue();
	    StringBuffer comment = new StringBuffer();
	    if ( probnum < 652 ) {
	      System.out.println("[B1] "+probnum+" "+link.getAttribute("indexstr")+", "+x);
	      node = traverseToTag(nextNode(node),new String[]{"DIV","SPAN","A","P","TD.sentence-std-go"},comment,url);
	      node = traverseToTag(node,new String[]{"H2","H3","IMG","UL","DIV"},strbuf,null);
	    } else {
	      System.out.println("[B2] "+probnum+" "+link.getAttribute("indexstr")+", "+x);
	      node = traverseToTag(nextNode(node),new String[]{"H2","H3","IMG","UL","DIV"},strbuf,null);
	      node = traverseToTag(node,new String[]{"UL","DIV"},comment,url);
	    }
	    int idx = strbuf.indexOf("ページトップ");
	    if ( idx > 0 ) strbuf.delete(idx,strbuf.length());
	    printContent(dumpout,strbuf,new URL(url,src),comment);
	    node = traverseToTag(node,new String[]{"H2","H3","STRONG"},strbuf,null);
	  }
	} else {
	  elems = response.getElementsByTagName("STRONG");
	  title = title+" "+elems[0].getText();
	  printTitle(dumpout,title);
	  for ( int x = 1; x < elems.length; ++x ) {
	    System.out.println("[A] "+link.getAttribute("indexstr")+", "+x);
	    Node node = elems[x].getNode();
	    StringBuffer strbuf = new StringBuffer();
	    node = traverseToTag(node,new String[]{"IMG"},strbuf,url);
	    String src = node.getAttributes().getNamedItem("src").getNodeValue();
	    StringBuffer comment = new StringBuffer();
	    node = traverseToTag(nextNode(node),new String[]{"STRONG","FONT","A"},comment,url);
	    printContent(dumpout,strbuf,new URL(url,src),comment);
	  }
	}
      }
    }
    dumpout.close();
  }

  public static void printTitle( PrintStream dumpout, String title )
  {
    dumpout.println("####"+title);
  }

  public static void printContent( PrintStream dumpout, StringBuffer text, URL image, StringBuffer comment )
  {
    dumpout.println(text);
    dumpout.println(image);
    dumpout.println(comment);
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

  public static Node traverseToTag( Node node, String tagNames[], StringBuffer strbuf, URL url )
  throws MalformedURLException
  {
    while ( node != null ) {
      String nodeName = node.getNodeName();
      for ( String tagName : tagNames ) {
	int idx = tagName.indexOf('.');
	if ( idx > 0 ) {
	  if ( node.hasAttributes() ) {
	    Node val = node.getAttributes().getNamedItem("class");
	    if ( 
	      val != null &&
	      nodeName.equals(tagName.substring(0,idx)) &&
	      val.getNodeValue().equals(tagName.substring(idx+1))
	    ) return node;
	  }
	} else {
	  if ( nodeName.equals(tagName) ) return node;
	}
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

  public static byte[][] createPage( String data[], int counts[], int from, int to )
  {
    ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
    ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
    PrintStream out1 = new PrintStream(bout1);
    PrintStream out2 = new PrintStream(bout2);

    printBoth(out1,out2,"<HTML>");
    out1.println("<TITLE>初級 "+(from/2+1)+"-"+(to/2)+"</TITLE>");
    out2.println("<TITLE>中級 "+(from/2+1)+"-"+(to/2)+"</TITLE>");
    printBoth(out1,out2,"<HEAD>");
    printBoth(out1,out2,"<meta name=viewport content=\"width=300\">");
    printBoth(out1,out2,"</HEAD>");
    printBoth(out1,out2,"<BODY>");

    for ( int i = from; i < to; i += 2 ) {
      int j = counts[i];
      printBoth(out1,out2,"<hr size=5 noshade>");
      printBoth(out1,out2,"<strong>"+data[j++]+"</strong>");
      printContent(out1,data[j++],data[j++],data[j++]);
      printContent(out2,data[j++],data[j++],data[j++]);
      printBoth(out1,out2,"<strong>"+data[j++]+"</strong>");
      while ( j < counts[i+1] ) {
	printContent(out1,data[j++],data[j++],data[j++]);
      }
      while ( j < counts[i+2] ) {
	printContent(out2,data[j++],data[j++],data[j++]);
      }
    }

    printBoth(out1,out2,"</BODY>");
    printBoth(out1,out2,"</HTML>");
    out1.close();
    out2.close();

    return new byte[][]{
      bout1.toByteArray(),
      bout2.toByteArray(),
    };
  }

  public static void printContent( PrintStream out, String text, String url, String comment )
  {
    out.println("<p>"+text+"</p>");
    out.println("<p><img width=280 src=\""+url+"\"></p>");
    out.println("<p>"+comment+"</p>");
  }

  public static void printBoth( PrintStream out1, PrintStream out2, String text )
  {
    out1.println(text);
    out2.println(text);
  }
}
