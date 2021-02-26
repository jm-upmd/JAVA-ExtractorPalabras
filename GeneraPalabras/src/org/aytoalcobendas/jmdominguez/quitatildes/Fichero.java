package org.aytoalcobendas.jmdominguez.quitatildes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class Fichero {
	
	static final int MODO_LECTURA = 0;
	static final int MODO_ESCRITURA = 1;
	
	private Scanner lector;
	//private FileWriter escritor;
	private BufferedWriter bWriter;	
	

	public Fichero(String nombre, int modo) throws IOException, FileNotFoundException {
		
		// Trabajamos com ficheros utf8
		
	
		switch (modo) {
		case MODO_LECTURA:
			lector = new Scanner(new File(nombre),StandardCharsets.UTF_8.name());		
			break;
		
		case MODO_ESCRITURA:
			
			bWriter = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(new File(nombre)), StandardCharsets.UTF_8.name()));
			break;

		default:
			System.out.println("Modo de apertura del fichero desconocido");
			System.exit(0);
		}

	}
	
	public Scanner getLector() {
		return lector;
	}
	
	public BufferedWriter getEscritor() {
		return bWriter;
	}
	
	public void cierra() {
		if(lector != null)
			lector.close();
		else  {
			
			try {
				if(bWriter !=null) bWriter.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
