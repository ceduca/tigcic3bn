package crawler.escalonadorCurtoPrazo;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.trigonic.jrobotx.Record;
import com.trigonic.jrobotx.RobotExclusion;

import crawler.Servidor;
import crawler.URLAddress;

public class EscalonadorSimples implements Escalonador{
	public  String USER_AGENT = "hasanBot";
	private  int pageLimit = 10;
	private int depthPerDomainLimit = 5;
	private RobotExclusion robotExclusion = new RobotExclusion();
	private List<Servidor> lstPilhaDominios = new ArrayList<Servidor>();
	private Map<String,Servidor> mapDominio = new HashMap<String,Servidor>();
	private Map<Servidor,List<URLAddress>> mapFilaURLPorDominio = new HashMap<Servidor,List<URLAddress>>();
	private Set<String> mapAddedURLs = new HashSet<String>();
	private Map<String,Record> recordPerDomain = new HashMap<String,Record>();
	
	private int pageCount =0;
	private long totalTime = System.currentTimeMillis();
	private long total100Pages = System.currentTimeMillis();
	private Map<String,String> domainIp = new HashMap<String,String>();
	
	
	public EscalonadorSimples(String userAgent, int pageLimit, int depthLimit)
	{
		this.USER_AGENT = userAgent;
		this.pageLimit = pageLimit;
		this.depthPerDomainLimit = depthLimit;
	}
	public synchronized URLAddress getURL()
	{
		//procura primeiro dominio acessivel (caso exista, caso nao exista, continue procurando) 
		Servidor d = null;
		URLAddress url = null;
		do
		{
			while(d == null)
			{
					d = getFirstDominioAcessivel();
					if(d == null)
					{
						try {
							System.out.println("A thread "+Thread.currentThread().getId()+" nao achou outro dominio...aguardando");
							this.wait(1000L);
							
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							
						}
					}
			}
			
			//resgata nova pagina deste dominio 
			if(mapFilaURLPorDominio.get(d).size()>0)
			{
				url = mapFilaURLPorDominio.get(d).remove(0);
			}
		}while(url == null);
		
		
		
		//define a hora de coleta deste servidor como agora
		d.acessadoAgora();
		
		return url;
	}

	private Servidor getFirstDominioAcessivel() {
		for(Servidor d : lstPilhaDominios)
		{
			if(d.isAccessible() && this.mapFilaURLPorDominio.get(d).size() > 0)
			{
				return d;
			}
		}
		return null;
	}
	public synchronized String getIpHost(String url) throws UnknownHostException, MalformedURLException
	{
		String domain = URLAddress.getDomain(url);
		if(this.domainIp.containsKey(domain))
		{
			return this.domainIp.get(domain);
		}else
		{
			InetAddress address = InetAddress.getByName(domain);
			String ip = address.getHostAddress();
			this.domainIp.put(domain, ip);
			
			return ip;
		}
	}
	
	int cont = 0;
	public synchronized boolean adicionaNovaPagina(URLAddress url)
	{
		/*
		if(cont>2)
		{
			return false;
		}
		cont++;
		*/
		if(mapAddedURLs.contains(url.getAddress())  || url.getDepth()>this.depthPerDomainLimit)
		{
			return false;
		}
		//try {
			//Servidor d = new Servidor(getIpHost(url.getAddress()));
			Servidor d = new Servidor(url.getDomain());
			if(this.mapFilaURLPorDominio.containsKey(d))
			{
				this.mapFilaURLPorDominio.get(d).add(url);
			}else
			{
				List<URLAddress> lstFilaUrls = new ArrayList<URLAddress>();
				mapFilaURLPorDominio.put(d, lstFilaUrls);
				
				lstFilaUrls.add(url);
				lstPilhaDominios.add(d);
			}
			mapAddedURLs.add(url.getAddress());
			this.notify();
			return true;
		//}
			/*catch (UnknownHostException e) {
			System.err.println("Nao achou o host: "+url.getDomain());
			return false;
		} catch (MalformedURLException e) {
			System.err.println("Nao achou o endereço: "+url.getAddress());
			return false;
		}*/
		
		
	}
	public synchronized void countFetchedPage()
	{
		this.pageCount = pageCount+1;
		if(this.pageCount%100 == 0)
		{
			System.out.println("Coletou 100 paginas em: "+(System.currentTimeMillis()-this.total100Pages)/1000.0+" seg");
			this.total100Pages = System.currentTimeMillis();
		}
	}
	public synchronized boolean finalizouColeta()
	{
		if(this.pageCount>=pageLimit)
		{
			double timeToColect = (System.currentTimeMillis()-this.totalTime)/1000.0;
			System.out.println("Coletou as "+pageCount+" paginas em: "+timeToColect+" seg");
			System.out.println(this.pageCount/timeToColect+" paginas por segundo");
			return true;
		}
		return false;
	}
	public Record getRecordAllowRobots(URLAddress url)
	{
		return recordPerDomain.get(url.getDomain());
	}
	public synchronized void putRecorded(String domain,Record r)
	{
		this.recordPerDomain.put(domain, r);
	}
	private synchronized boolean isAllowedRobots(URLAddress url) {
		// TODO Auto-generated method stub
		Record domainRec = null;
		if(!recordPerDomain.containsKey(url.getDomain()))
		{
			//System.out.println("Nao achou um dominio: "+url.getDomain());
			try {
				domainRec = robotExclusion.get(new URL(url.getProtocol()+"://"+url.getDomain()+"/robots.txt"), USER_AGENT);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				return true;
			}
			this.recordPerDomain.put(url.getDomain(), domainRec);
			
			
		}else
		{
			domainRec = recordPerDomain.get(url.getDomain());
		}
		if(domainRec == null)
		{
			return true;
		}
		return domainRec.allows(url.getPath());
	}
	public void printURLsPerServers()
	{
		int servCol = 0;
		
		for(Servidor s : this.lstPilhaDominios)
		{
			//List<URLAddress> lstAddress = this.mapFilaURLPorDominio.get(s);
			//System.out.print("Servidor: "+s.getNome()+" Ultima coleta em: "+(s.getTimeSinceLastAcess()/1000.0)+" seg Numero de urls deste servidor: "+lstAddress.size());
			//System.out.println("Nome do host: "+lstAddress.get(0).getDomain());
			if(s.getLastAcess()!=0)
			{
				servCol++;
			}
		}
		System.out.println("Qtd de servidores: "+servCol);
		
		
	}
	
	
	public static void main(String[] args) throws MalformedURLException, InterruptedException
	{
		
		EscalonadorSimples esq = new EscalonadorSimples("hasanBot",500,5);
		
		/*
		esq.adicionaNovaPagina(new URLAddress("http://www.band.uol.com.br/tv/", 0));
		esq.adicionaNovaPagina(new URLAddress("http://www.band.uol.com.br/clube/", 0));
		*/
		
		esq.adicionaNovaPagina(new URLAddress("http://www.globo.com.br/", 0));
		esq.adicionaNovaPagina(new URLAddress("http://www.uol.com.br/", 0));
		esq.adicionaNovaPagina(new URLAddress("http://www.amazon.com/", 0));
		
		
		Thread[] arrT = new Thread[2];
		for(int i =0; i<arrT.length ; i++)
		{
			PageFetcher pf1 = new PageFetcher(esq);
			arrT[i] = new Thread(pf1);
			arrT[i].start();
		}
		for(int i =0; i<arrT.length ; i++)
		{
			arrT[i].join();			
		}
		
		

		
		esq.printURLsPerServers();
		
	
		
			
		
		/*
		RobotExclusion robotExclusion = new RobotExclusion();
		
		Record rFB = robotExclusion.get(new URL("https://www.facebook.com/robots.txt"),"daniBot");
		System.out.println("Aceitou o fb index?  "+rFB.allows("/index.html"));
		System.out.println("Aceitou o fb o cgi-bin?  "+rFB.allows("/cgi-bin/oioi"));
		System.out.println("Aceitou o fb o oioi?  "+rFB.allows("/lala/oioi"));
		
		Record rTerra = robotExclusion.get(new URL("http://www.terra.com.br/robots.txt"),"daniBot");
		System.out.println("Aceitou o terra index?  "+rTerra.allows("/index.html"));
		System.out.println("Aceitou o terra o cgi-bin?  "+rTerra.allows("/cgi-bin/oioi"));
		System.out.println("Aceitou o terra o oioi?  "+rTerra.allows("/lala/oioi"));		
		*/
	}
}
