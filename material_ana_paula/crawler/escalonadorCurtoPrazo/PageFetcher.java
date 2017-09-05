package crawler.escalonadorCurtoPrazo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.mozilla.universalchardet.UniversalDetector;

import com.trigonic.jrobotx.Record;
import com.trigonic.jrobotx.RobotExclusion;

import crawler.URLAddress;

public class PageFetcher implements Runnable
{
	private Escalonador escalonador = null;
	private RobotExclusion robotExclusion = new RobotExclusion();
	public PageFetcher(Escalonador esc)
	{
		this.escalonador = esc;
	}
	
	/**
	 * altera o buffer, se necessário
	 * Extraido de: http://codingwiththomas.blogspot.com.br/2013/01/build-basic-crawler.html
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	private static ByteBuffer resizeBuffer(ByteBuffer buffer) {
	  ByteBuffer result = buffer;
	  // double the size if we have only 10% capacity left
	  if (buffer.remaining() < (int) (buffer.capacity() * 0.1f)) {
	    result = ByteBuffer.allocate(buffer.capacity() * 2);
	    buffer.flip();
	    result.put(buffer);
	  }
	  return result;
	}
	
	/**
	 * Coleta a pagina de acordo com o encoding
	 * Extraido de: http://codingwiththomas.blogspot.com.br/2013/01/build-basic-crawler.html
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static String consumeStream(InputStream stream) throws IOException {
		  try {
		    // setup the universal detector for charsets
		    UniversalDetector detector = new UniversalDetector(null);
		    ReadableByteChannel bc = Channels.newChannel(stream);
		    
		    
		    // 1mb is enough for every usual webpage
		    ByteBuffer buffer = ByteBuffer.allocate(1024*1024);
		    int read = 0;
		    while ((read = bc.read(buffer)) != -1) {
		      // let the detector work on the downloaded chunk
		      detector.handleData(buffer.array(), buffer.position() - read, read);
		      // check if we found a larger site, then resize the buffer
		      buffer = resizeBuffer(buffer);
		    }
		    // finish the sequence
		    detector.dataEnd();
		    
		    // copy the result back to a byte array
		    String encoding = detector.getDetectedCharset();
		    
		    // obtain the encoding, if null fall back to UTF-8
		    return new String(buffer.array(), 0, buffer.position(),
		        encoding == null ? "UTF-8" : encoding);
		  } finally {
		    if (stream != null) {
		      stream.close();
		    }
		  }
		}
	private static boolean isAbsoluteURL(String urlString)
    {
        boolean result = false;
        try
        {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (protocol != null && protocol.trim().length() > 0)
                result = true;
        }
        catch (MalformedURLException e)
        {
            return false;
        }
        return result;
    }
	private List<URLAddress> getLinks(URLAddress urlOrigem, String text)
	{
		final List<URLAddress> lstURLs = new ArrayList<URLAddress>();
		final int depth = urlOrigem.getDepth();
		final String domain = urlOrigem.getDomain();
		final URLAddress urlOrigemM = urlOrigem;
		
		//CleanerProperties props = new CleanerProperties();
		HtmlCleaner cleaner = new HtmlCleaner();
		//HtmlCleaner cleaner = new HtmlCleaner();
		
		TagNode node = cleaner.clean(text);
		
		// traverse whole DOM and update images to absolute URLs
		node.traverse(new TagNodeVisitor() {
		    public boolean visit(TagNode tagNode, HtmlNode htmlNode) {
		        if (htmlNode instanceof TagNode) {
		            TagNode tag = (TagNode) htmlNode;
		            String tagName = tag.getName();
		            if ("a".equalsIgnoreCase(tagName)) {
						String ref = tag.getAttributeByName("href");
						if(ref!=null)
						{
							if(!isAbsoluteURL(ref))
							{
								ref = urlOrigemM.getAddress()+"/"+ref;
							}
	
			            	try {
								
								int urlDepth = 0;
								if(domain.equalsIgnoreCase(URLAddress.getDomain(ref)))
								{
									urlDepth = depth +1;
								}
								lstURLs.add(new URLAddress(ref, urlDepth));
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								System.err.println("URL: "+ref+" Mal formada. Dominio pai: "+urlOrigemM.getAddress());
							}
						}
		            }
		        }
		        // tells visitor to continue travering the DOM tree
		        return true;
		    }
		});
		
		return lstURLs;
		
	}
		//
	private String getURLContent(URLAddress urlAddress)
	{
		URL url;
	    InputStream is = null;
	    try {
	        url = new URL(urlAddress.getAddress());

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//conn.setRequestProperty("User-Agent","botTest teste/info");
			
			is = conn.getInputStream();  // throws an IOException
	        
	        String pageText = consumeStream(is);
	        is.close();
	        
	    
	        return pageText;
	        
	    } catch (MalformedURLException mue) {
	         //mue.printStackTrace();
	    } catch (IOException ioe) {
	         //ioe.printStackTrace();
	    } finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            // nothing to see here
	        }
	    }
	    return null;
	}
	public boolean isAllowedRobots(URLAddress url)
	{
		Record domainRec =  escalonador.getRecordAllowRobots(url);
		if(domainRec == null)
		{
			//System.out.println("Nao achou um dominio: "+url.getDomain());
			try {
				System.out.println(Thread.currentThread().getId()+" Robots: "+new URL(url.getProtocol()+"://"+url.getDomain()+"/robots.txt").toString());
				domainRec = robotExclusion.get(new URL(url.getProtocol()+"://"+url.getDomain()+"/robots.txt"), "*");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				return true;
			}
			this.escalonador.putRecorded(url.getDomain(), domainRec);
			
			
		}
		if(domainRec == null)
		{
			return true;
		}
		return domainRec.allows(url.getPath());
	}
	public synchronized void crawlNewURL()
	{
		URLAddress url = this.escalonador.getURL();
		System.out.println("Coletando: "+url.getAddress());
		if(this.isAllowedRobots(url))
		{
			//pagina
			try {
				String text = getURLContent(url);
				if(text != null)
				{
					//caso ela exista, extrai seus links e os adiciona no escalonador
					List<URLAddress> lstLinks = getLinks(url, text);
					
					//adicona todos os links no escalonador
					int numNewPages = 0;
					for(URLAddress urlFilha : lstLinks)
					{
						//System.out.println("URL: "+urlFilha.getAddress());
						if(escalonador.adicionaNovaPagina(urlFilha))
						{
							numNewPages++;
							
						}
					}
					System.out.println("Numero de url adicionadas: "+numNewPages);
					escalonador.countFetchedPage();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}else
		{
			System.out.println("Nao autorizado coletar: "+url.getAddress());
		}
		
		
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!escalonador.finalizouColeta())
		{
			crawlNewURL();
		}
		System.out.println("Thread "+Thread.currentThread().getId()+" Finalizou!");
		
	}
	public static void main(String[] args) throws MalformedURLException
	{
		
		
		
		PageFetcher pf = new PageFetcher(new Escalonador() {
		int count = 0;	
			@Override
			public URLAddress getURL() {
				// TODO Auto-generated method stub
				try {
					return new URLAddress("https://www.facebook.com", 0);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
			
			@Override
			public boolean finalizouColeta() {
				// TODO Auto-generated method stub
				return count!=0;
			}
			
			@Override
			public void countFetchedPage() {
				// TODO Auto-generated method stub
				count++;
				System.out.println("contou!!");
			}
			
			@Override
			public boolean adicionaNovaPagina(URLAddress urlAdd) {
				// TODO Auto-generated method stub
				//System.out.println("URL Nova: "+urlAdd.getAddress());
				return true;
			}

			@Override
			public Record getRecordAllowRobots(URLAddress url) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void putRecorded(String domain, Record domainRec) {
				// TODO Auto-generated method stub
				
			}
		});
		pf.isAllowedRobots(new URLAddress("http://www.band.uol.com.br/", 0));
		pf.isAllowedRobots(new URLAddress("http://www.band.uol.com.br/", 0));
		pf.isAllowedRobots(new URLAddress("http://www.band.uol.com.br/", 0));
		/*
		Thread t = new Thread(pf);
		t.start();
		*/
	}
	
}
