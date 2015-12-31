package mylib.hitigo;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;

public class CreateMainTest
{
  // 下記のは古い。もう使用しない。
  // 次のように起動する
  // build -Dserver=server.name,id,pw,folder test
  //@Test
  public void testMain()
  throws Exception
  {
    String prop = System.getProperty("server");
    if ( prop == null ) {
      try {
	byte data[][] = CreateMain.createPage1(1996);
	System.out.write(data[0]);
	System.out.write(data[1]);
      } catch ( Exception ex ) {
	System.out.println("-- response -- start");
	System.out.println(CreateMain.lastPage);
	System.out.println("-- response -- end");
	throw ex;
      }
    } else {
      //CreateMain.main(prop.split(","));
      CrawlMain.main(prop.split(","));
    }
  }

  // build test で実行するためのもの。
  //@Test
  public void testRun()
  throws Exception
  {
    CrawlMain.main(new String[0]);
  }

  @Test
  public void testNumber()
  throws Exception
  {
    String str = "第652回 出題（答え）";
    str = str.replaceAll("[^0-9]","");
    assertEquals("652",str);
    int num = Integer.parseInt(str);
    assertEquals(652,num);
  }
}
