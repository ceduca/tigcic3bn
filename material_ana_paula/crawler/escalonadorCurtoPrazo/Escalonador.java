package crawler.escalonadorCurtoPrazo;

import com.trigonic.jrobotx.Record;

import crawler.URLAddress;

public interface Escalonador {
	
	/**
	 * Metodo para resgatar uma url. Não esquecer que, ao implementar esse método em que os PageFetcher são multithread, 
	 * voce deve implementar este metodo com o termo "synchronized", para utilizar o monitor e deixar
	 * esta classe threadsafe 
	 * @return
	 */
	public URLAddress getURL();
	
	
	/** 
	 * Adiciona uma nova url ao escalonador. Não esquecer que, ao implementar esse método em que os PageFetcher são multithread, 
	 * voce deve implementar este metodo com o termo "synchronized", para utilizar o monitor e deixar
	 * esta classe threadsafe  
	 * @return
	 */
	public boolean adicionaNovaPagina(URLAddress urlAdd);

	public boolean finalizouColeta();
	
	
	public void countFetchedPage();


	public Record getRecordAllowRobots(URLAddress url);


	public void putRecorded(String domain, Record domainRec);
}
