package crawler;

class Contador
{
	private int cont = 0;
	public void incContador()
	{
		for(int i = 0; i<100000; i++)
		{
			cont = cont++;
		}
	}
	public void decContador()
	{
		
		for(int i = 0; i<100000; i++)
		{
			cont = cont--;
		}
	}
	public int getCount()
	{
		return cont;
	}
}
public class MultiThreadHelloWorld  implements Runnable{
	private Contador count;
	private boolean inc;
	public MultiThreadHelloWorld(Contador count,boolean inc)
	{
		this.count = count;
		this.inc = inc;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(inc)
		{
			count.incContador();
		}else
		{
			count.decContador();
		}
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		for(int i = 0 ; i<10 ; i++)
		{
			Contador cont = new Contador();
			MultiThreadHelloWorld mHello1 = new MultiThreadHelloWorld(cont, true);
			MultiThreadHelloWorld mHello2 = new MultiThreadHelloWorld(cont, false);
			
			Thread t1 = new Thread(mHello1);
			Thread t2 = new Thread(mHello2);
			//inicializa threads
			t1.start();
			t2.start();
			//espera terminar
			t1.join();
			t2.join();
			System.out.println("Resultado: "+cont.getCount());
		}
	}

}
