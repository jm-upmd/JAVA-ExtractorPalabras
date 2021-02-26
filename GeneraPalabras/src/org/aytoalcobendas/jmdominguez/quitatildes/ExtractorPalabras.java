/*
 * Sintaxis ejecución: java -D"file.encoding=UTF-8" -jar quitaTildes.jar <f_corresp> <f_entrada>)
 */

package org.aytoalcobendas.jmdominguez.quitatildes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import gnu.getopt.Getopt;

public class ExtractorPalabras {

	static int tamanoMinPalabra;

	// Nombre de los ficheros. Se dan como parámetros al programa.
	// Si el tercero (el de salida) no se da como parámetro, entonces se genera
	// automáticamente a partir de fichEntrada

	static String fichConversion = null;
	static String fichEntrada = null;
	static String fichSalida = null;

	// Objeto Fichero para cada uno

	static Fichero fConversion = null;
	static Fichero fEntrada = null;
	static Fichero fSalida = null;

	// Tabla de correspondencia para sustitución de caracteres
	// Sin duplicados y ordenados
	static TreeMap<Character, Character> tc = new TreeMap<>();

	public static void main(String[] args) {
		Long tiempo = System.currentTimeMillis();

		Scanner sc = null;

		// Lista de palabras seleccionadas (sin duplicados y no ordenada)
		HashSet<String> listaPalabras = new HashSet<>();

		// Recogida de parámetros de entrada y procesamiento

		procesParametros(args);

		// Si se ha dado nom fich salida como parámetro, se construye a partir del de
		// entrada

		if (fichSalida == null)
			ponNombreFiSalida();

		// Crea objetos ficheros
		boolean salYaExiste = false; // flag que indica si el fichero salida ya existe o no
		try {

			// Fichero de conversión
			fConversion = new Fichero(fichConversion, Fichero.MODO_LECTURA);

			// Fichero de entrada
			fEntrada = new Fichero(fichEntrada, Fichero.MODO_LECTURA);

			// Fichero de salida

			// Si el fichero de salida existe carga las palabras que ya contiene en
			// listaPalabras

			if (new File(fichSalida).exists()) {
				salYaExiste = true;
				System.out.println("Fichero de salida ya existe. Se añadirán nuevas palabras.");
				fSalida = new Fichero(fichSalida, Fichero.MODO_LECTURA);
				sc = fSalida.getLector();
				while (sc.hasNextLine()) {
					listaPalabras.add(sc.nextLine());
				}
				fSalida.cierra();

			}

			// Abre en modo escritura para luego guardar la lista de palabras.
			// La lista será nueva o con palabras añadidas sobre las ya existentes en
			// fichero si este ya existía.
			fSalida = new Fichero(fichSalida, Fichero.MODO_ESCRITURA);

		} catch (FileNotFoundException e) {
			System.out.println("No se encuentra el fichero: " + e.getLocalizedMessage());
			System.exit(0);
		} catch (IOException e) {
			System.out.println(
					"Se ha producido un error durante la operación de lect/esc del fichero" + e.getLocalizedMessage());
			System.exit(0);
		}

		// Carga tabla de correspondencia
		// Reutilizamos la variable sc de tipo Scanner
		if (sc != null)
			sc.close();
		sc = fConversion.getLector();
		while (sc.hasNextLine()) {
			String linea = sc.nextLine();
			String[] a = linea.split(",");
			tc.put(a[0].charAt(0), a[1].charAt(0));
		}

		fConversion.cierra(); // Cierra fichero de tabla corresp.

		// Abre fichero de entrada del cual extraemos palabras

		sc = fEntrada.getLector();

		// Abre fichero salida en que se guardará la lista de palabras

		BufferedWriter bw = fSalida.getEscritor();

		// Procesa fichero de entrada

		System.out.println("Comienza proceso...");
		int palNueva = 0;

		// Crea expresión regular para filtrar palabras que queremos extraer
		// palabras en minúscula que pueden o no tener un signo de puntuación al final
		// y cuyo tamaño es al menos tamanoMinPalabra

		String expreg = "^[a-zñáéíóúü]{" + tamanoMinPalabra + ",}(?=\\p{Punct}?)";
		Pattern palOK = Pattern.compile(expreg);
		Matcher matcher;
		String palabra;
		while (sc.hasNext()) { // Mientras hay palabras para leer en fich entrada
			palabra = sc.next();
			 matcher = palOK.matcher(palabra);
			if (matcher.find()) {
				if (listaPalabras.add(transformaLetras(matcher.group())))
					palNueva++; // Contandor de nueva palabra añadidida
			}
		} // while

		try {

			// Graba el HasSet con la lista de palabras extradidas en el fichero de salida.

			for (String pal : listaPalabras) {
				bw.write(pal);
				bw.newLine();
			}

			// Cierra ambos ficheros

			fEntrada.cierra();
			fSalida.cierra();

		} catch (IOException e) {
			System.out.println("Se ha producido un error mientras se leía/escribía en el fichero");
			System.out.println(e.getLocalizedMessage());
		}

		System.out.println("Proceso terminado (tiempo: " + (System.currentTimeMillis() - tiempo) +" msg.)");
		System.out.println("Total palabras: " + listaPalabras.size());
		System.out.println("Extraidas palabras con longitud de al menos " + tamanoMinPalabra + " letras.");
		if (salYaExiste)
			System.out.println("Total palabras nuevas: " + palNueva);
		System.out.println("Fichero: " + fichSalida);
		
	}

	/**
	 * Transforma una palabra convirtiendo todos sus caracteres en mayúsculas y
	 * todas las vocales con tilde o diéresis en sus analogas sin puntuación.
	 * 
	 * @param pal Palabra a transformar
	 * @return Palabra transformada
	 */

	static String transformaLetras(String pal) {
		Character cs;
		StringBuilder palabra = new StringBuilder(pal);

		for (int i = 0; i < palabra.length(); i++) {

			// Si tiene tilde o dieresis
			if ((cs = tc.get(palabra.charAt(i))) != null)
				palabra.setCharAt(i, cs);

			palabra.setCharAt(i, Character.toUpperCase(palabra.charAt(i)));
		}

		return palabra.toString();
	}

	static void procesParametros(String[] param) {
		/*
		 * Parámetros: -c <f_conv> -e <f_ent> [-s <f_sal>] -n <min>
		 */

		// Usamos librería Getopt de libre distribución para recoger y gestionar los
		// parámetros
		// de entrada. Es más cómodo que hacerlo a mano. Hay mas libreriás para hacer
		// esto.

		Getopt g = new Getopt("extraePalabras", param, "c:e:s:n:");

		int c;
		String arg;
		boolean flagc, flage, flagn;
		flagc = flage = flagn = false;

		while ((c = g.getopt()) != -1) {
			switch (c) {
			case 'c':
				fichConversion = g.getOptarg();
				flagc = true; // flag que indica que el parametro se ha dado
				break;
			case 'e':
				fichEntrada = g.getOptarg();
				flage = true;
				break;
			case 's':
				fichSalida = g.getOptarg();
				break;
			case 'n':
				arg = g.getOptarg();
				if (esNumero(arg)) {
					tamanoMinPalabra = Integer.parseInt(arg);
				} else
					errorParam(c, arg, "Parámetro no es un número entero");

				flagn = true;

				break;

			case '?':
				errorParam(g.getOptopt(), g.getOptarg(), "Opción no valida");

			}
		}

		// Estos tres parámetros son obligatorios. Comprueba que se han dado todos
		if (!(flagc && flage && flagn))
			errorParam((int) ' ', "", "Parámetros incorrectos");

	}

	static void errorParam(int op, String arg, String causa) {
		System.out.printf("Error en parámetro de entrada: -%s %s\n", (char) op, arg == null ? "" : arg);
		System.out.println(causa);
		imprimeSintaxis(); // lee e imprime fichero de recurso.
		System.exit(0);

	}

	static void imprimeSintaxis() {

		// Leemos fichero de recurso. Estos ficheros forman parte del programa.
		// Están dentro del classpath del programa. getResource lo busca dentro del
		// claspath
		// Se empaquetan dentro del jar junto con los class, y demás recursos del
		// programa.

		InputStream is = null;
		Reader isr = null;
		BufferedReader br = null;
		try {
			is = ExtractorPalabras.class.getResourceAsStream("/" + "sintaxis.txt");
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr);

			String line;

			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}

		} catch (IOException e) {
			System.out.print("Error al leer fichero de ayuda");
		} finally {
			try {
				br.close();
				isr.close();
				is.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}

	}

	static boolean esNumero(String n) {

		try {
			@SuppressWarnings("unused") // suprime warning por no utilizar num
			int num = Integer.parseInt(n);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	static void ponNombreFiSalida() {

		// Construye nombre del fichero se salida igual que el de entrada pero con
		// sufijo "_SALIDA" en el nombre
		// y manteniendo la misma extensión: mi_fichero.txt --> mi_fichero_SALIDA.txt
		// Si no tiene extensión: mi_fichero --> mi_fichero_SALIDA

		String nombreSinExt = FilenameUtils.removeExtension(FilenameUtils.getName(fichEntrada)); 
		String extension = FilenameUtils.getExtension(fichEntrada);
		String directorio = FilenameUtils.getFullPath(fichEntrada);

		fichSalida = directorio + nombreSinExt + "_SALIDA" + (extension.equals("") ? "" : "." + extension);
	}
}
