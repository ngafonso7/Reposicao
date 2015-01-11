package br.natanael.reposicao;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	ListView listaProd;
	
	Button botaoAtualiza;
	
	//Server local for tests
	//static String ipServer = "192.168.0.174";
	static String ipServer = "192.168.0.125";
    static String portaServer = "12345";
	
    static Socket server;
    
    InputStream entrada = null;
    OutputStream saida = null;
	
    Handler handler;
    
    static ProgressDialog progressBar;
    private boolean progressBarStatus = false;
    private Handler progressBarHandler = new Handler();
    
    ArrayList<String> produtos;
    ArrayList<String> dataProduto;
    ArrayList<String> codigoProduto;
    ArrayList<String> descricaoProduto;
    ArrayList<String> quantidadeProduto;
    
    public Boolean testServer = true;
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		handler = new Handler() {
			  @Override
			  public void handleMessage(Message msg) {
				 
				  	Bundle bundle = new Bundle();
				  	bundle = msg.getData();
				  	if(bundle.get("cmd").toString().compareTo("MSG")==0)
				  	{
					  	showErro(bundle.get("msg").toString(),bundle.get("title").toString() ,false);
					  	
					  	if(bundle.getBoolean("FIM"))
					  	{
					  		aguarde_end();
					  	}
				  	}
				  	else if(bundle.get("cmd").toString().compareTo("MSG-F")==0)
				  	{
					  	showErro(bundle.get("msg").toString(),bundle.get("title").toString() ,true);
				  	}
				  	else if(bundle.get("cmd").toString().compareTo("FillList")==0)
				  	{
				  		ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1, produtos);
						listaProd.setAdapter(adapter);
				  	}
				  	
			     }
			 };
			 
		listaProd = (ListView)findViewById(R.id.listaProd );
		botaoAtualiza = (Button) findViewById(R.id.botaoAtualizar);
		
		conectar(ipServer, portaServer);
		
		aguarde_end();
		
		produtos = new ArrayList<String>();
		dataProduto = new ArrayList<String>();
		codigoProduto = new ArrayList<String>();
		descricaoProduto = new ArrayList<String>();
		quantidadeProduto = new ArrayList<String>();
		
		progressBar = new ProgressDialog(MainActivity.this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Aguarde ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(20);
		
		atualizaListaProdutos();
		
		botaoAtualiza.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				atualizaListaProdutos();
				
			}
		});
		
		 listaProd.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,int index, long arg3) {
					final int ind = index;
					
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			        builder
			        	.setTitle("Opções do Produto")
			    		.setMessage("Deseja dar baixa no produto ?")
			            .setNegativeButton("Dar Baixa", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int which) {
			                	aguarde_init();
			            		Thread th = new Thread(new Runnable() {
			            			
			            			@Override
			            			public void run() {
			            				conectar(ipServer,portaServer);
			            				enviar("BRP\n"+dataProduto.get(ind)+";"+codigoProduto.get(ind)+";"+quantidadeProduto.get(ind)+"\n");
			            				receber();
			            				aguarde_end();
			            				
			            			}
			            		});
			            		th.start();
			                	
			                }
			            })
			            .setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
			                public void onClick(DialogInterface dialog, int which) {
			                	
			                	
			                }
			            }).show();
					return false;
				}
		 });
	}
	
	public void atualizaListaProdutos()
	{
		aguarde_init();
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run() {
				conectar(ipServer,portaServer);
				enviar("CRP\n");
		        receber();
				aguarde_end();
			}
		});
		th.start();
		
	}
	
    public void receber(){
        try{
            //aguarde_init();
            if(this.server != null)
            {
                entrada = server.getInputStream();
                if(this.entrada != null)
                {
                    String palavra="";
                    palavra = recebeCaracteres();
                    palavra.replace("\n","");
                    
                    produtos.clear();
                	dataProduto.clear();
                    codigoProduto.clear();
                    descricaoProduto.clear();
                    quantidadeProduto.clear();
                    
                    if(palavra.compareTo("Encontrado")==0)
                    {
                    	palavra="";
                        
                        while((palavra = recebeCaracteres()).compareTo("CRP")==0)
                        {
                        	String dados[] = recebeCaracteres().split(";");
                        	produtos.add(dados[0] + " - " +dados[1] + " - " + dados[2] + " - " + dados[3]);
                        	dataProduto.add(dados[0]);
                        	codigoProduto.add(dados[1]);
                        	descricaoProduto.add(dados[2]);
                        	quantidadeProduto.add(dados[3]);
                        }
                        
                        Message msg = handler.obtainMessage();
                    	Bundle bundle = new Bundle();
                    	bundle.putString("cmd", "FillList");
                    	msg.setData(bundle);
                    	handler.sendMessage(msg);
                        
                    }
                    else if(palavra.compareTo("Baixa OK")==0)
                    {
                    	atualizaListaProdutos();
                    }
                    else if(palavra.compareTo("CRPF")==0)
                    {
                    	Message msg = handler.obtainMessage();
                    	Bundle bundle = new Bundle();
                    	bundle.putString("cmd", "FillList");
                    	msg.setData(bundle);
                    	handler.sendMessage(msg);
                    }
                }
            }
        }
        catch(Exception e)
        {
        	Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }
	
	public void conectar(String ip, String porta){
        try{

            server = new Socket(ip,Integer.parseInt(porta));

            entrada = server.getInputStream();
            saida = server.getOutputStream();
            if(testServer)
            {
                enviar("Test Server");
                Thread.sleep(500);
                testServer = false;
            }

            String palavra = "";

            Boolean conexaoOk = false;

            while(!conexaoOk)
            {
            	palavra = recebeCaracteres();
                conexaoOk = true;
                if(palavra.compareTo("Conexao OK")!=0)
                {
                	//showErro("Servidor não respondeu !","Sem conexão",true);
                    //System.exit(0);
                	Message msg = handler.obtainMessage();
                	Bundle bundle = new Bundle();
                	bundle.putString("cmd", "MSG-F");
                	bundle.putString("msg", "Erro na conexão com Servidor");
                	bundle.putString("title", "Conexão Servidor");
                	bundle.putBoolean("FIM", true);
                	msg.setData(bundle);
                	handler.sendMessage(msg);
                }
            }

        }catch(Exception e){
            //showErro("Servidor não respondeu !","Sem conexão",true);
        	Message msg = handler.obtainMessage();
        	Bundle bundle = new Bundle();
        	bundle.putString("cmd", "MSG-F");
        	bundle.putString("msg", "Erro na conexão com Servidor");
        	bundle.putString("title", "Conexão Servidor");
        	bundle.putBoolean("FIM", true);
        	msg.setData(bundle);
        	handler.sendMessage(msg);

        }

    }
	
	public void enviar(String msg){
        try{
            if(server != null)
            {
                byte[] b = (msg+"\n").getBytes();

                if(saida != null)
                {

                    saida.write(b);
                    saida.flush();
                }
            }
        }catch(Exception e){
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG ).show();
        }
    }
	
	public String recebeCaracteres() throws Exception
    {
    	int res = -1;
    	Boolean recebendo = true;
        String palavra="";
        while(recebendo)
        {
            res = entrada.read();
            if((char)res != '\n' & res != -1)
            {
                palavra += (char)res;
            }
            else if (res == -1)
            {
            	recebendo = false;
            	palavra =  "Falha";
            }
            else
            {
            	recebendo = false;
            }
        }
        return palavra;
    }
	
	public void showErro(String msg,final Boolean exit)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
        	.setTitle("Reposição")
    		.setMessage(msg)
            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	if(exit)
                    	System.exit(0);
                	
                }
            })
            .show();
    }
    
    public void showErro(String msg,String titulo,final Boolean exit)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
        	.setTitle(titulo)
    		.setMessage(msg)
            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                        if(exit)
                        	System.exit(0);
                        
                }
            })
            .show();
    }
	
	
	public void aguarde_init(){
        // prepare for a progress bar dialog

        progressBar.show();
        
        //reset progress bar status
        progressBarStatus = true;
        new Thread(new Runnable() {
            public void run() {
                while (progressBarStatus==true) {
                    // your computer is too fast, sleep 1 second
                    //try {
                    //    Thread.sleep(1000);
                    //} catch (InterruptedException e) {
                    //    e.printStackTrace();
                    //}
                    // Update the progress bar
                    progressBarHandler.post(new Runnable() {
                        public void run() {
                            progressBar.setProgress(0);
                        }
                    });
                }
                progressBar.dismiss();
            }
        }).start();

    }
    public void aguarde_end(){
    	//progressBar.dismiss();
        progressBarStatus = false;
    }



}
