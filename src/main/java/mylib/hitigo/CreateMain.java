package mylib.hitigo;

import com.meterware.httpunit.ClientProperties;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HTMLElement;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.TableRow;
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebImage;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
      String server = args[0];
      String loginid = args[1];
      String password = args[2];
      String folder = args[3];

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

      // クロール＆ファイル生成
      byte data[][] = createPage1();

      // ファイル出力
      if ( !ftpclient.storeFile(folder+"/igo-1996a.html",new ByteArrayInputStream(data[0])) ) {
	throw new IOException("store file failure : igo-1996a.html");
      }
      if ( !ftpclient.storeFile(folder+"/igo-1996b.html",new ByteArrayInputStream(data[1])) ) {
	throw new IOException("store file failure : igo-1996b.html");
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

  public static byte[][] createPage1()
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

    request = new GetMethodWebRequest("http://www.hitachi.co.jp/Sp/tsumego/past/1996.html");
    response = wc.getResponse(request);

    WebTable table = response.getTables()[2];
    table = table.getTableCell(0,0).getTables()[2];
    int rows = table.getRowCount();
    int cols = table.getColumnCount();
    out1.println("<HTML>");
    out1.println("<TITLE>1996年 初級</TITLE>");
    out1.println("<HEAD>");
    out1.println("<meta name=viewport content=\"width=300\">");
    out1.println("</HEAD>");
    out1.println("<BODY>");
    out2.println("<HTML>");
    out2.println("<TITLE>1996年 中級</TITLE>");
    out2.println("<HEAD>");
    out2.println("<meta name=viewport content=\"width=300\">");
    out2.println("</HEAD>");
    out2.println("<BODY>");
    for ( int i = 0; i < rows; ++i ) {
      for ( int j = 0; j < cols; ++j ) {
	TableCell cell = table.getTableCell(i,j);
	for ( String type : new String[] { "問題", "答え" } ) {
	  WebLink link = cell.getLinkWith(type);
	  request = link.getRequest();
	  response = wc.getResponse(request);
	  lastPage = response.getText();
	  HTMLElement elems[] = response.getElementsByTagName("STRONG");
	  out1.println("<strong>"+type+" "+elems[0].getText()+"</strong>");
	  out2.println("<strong>"+type+" "+elems[0].getText()+"</strong>");
	  for ( int x = 1; x < elems.length; ++x ) {
	    System.out.println("page "+i+", "+j+", "+x);
	    Node node = elems[x].getNode();
	    StringBuffer strbuf = new StringBuffer();
	    node = traverseToTag(node,"IMG",strbuf);
	    String url = node.getAttributes().getNamedItem("src").getNodeValue();
	    PrintStream out = strbuf.indexOf("初級") > 0 ? out1 : out2;
	    out.println("<p>"+strbuf+"</p>");
	    out.println("<p><img width=280 src=\""+new URL(request.getURL(),url)+"\"></p>");
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

  public static Node traverseToTag( Node node, String tagName, StringBuffer strbuf )
  {
    while ( node != null && !node.getNodeName().equals(tagName) ) {
      if ( node instanceof Text ) strbuf.append(((Text)node).getData());
      Node next;
      if ( (next = node.getFirstChild()) == null ) {
	while ( node != null && (next = node.getNextSibling()) == null ) {
	  node = node.getParentNode();
	}
      }
      node = next;
    }
    return node;
  }
}
