package peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import model.Request;

@SuppressWarnings("serial")
public class Peer implements Serializable{

	private String peerIP;
	private int peerListeningPort;
	private List<File> peerFiles;
	private String peerDirectory;
	private String searchedFile;
	private List<String> searchedPeers;

	// Construtor
	public Peer(String peerIP, int peerListeningPort, String peerDirectory, List<File> peerFiles) {
		
		this.peerIP = peerIP;
		this.peerListeningPort = peerListeningPort;
		this.peerDirectory = peerDirectory;
		this.peerFiles = peerFiles;
	}

	// Getters
	public String getPeerIP() {
		
		return peerIP;
	}

	public int getPeerListeningPort() {
		
		return peerListeningPort;
	}

	public List<File> getPeerFiles() {
		
		return peerFiles;
	}

	// Cria a classe aninhada PortListenerThread
	private class PortListenerThread extends Thread {

		private Socket clientSocket;
		private String peerDirectory;

		// Construtor
		public PortListenerThread(Socket clientSocket, String peerDirectory) {
			
			this.clientSocket = clientSocket;
			this.peerDirectory = peerDirectory;
		}
		
		// RUN
		@Override
		public void run() {
			
			try {
				receiveDownloadRequest(clientSocket, peerDirectory); // Envia os dados do peer cliente para o método do peer servidor

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Cria uma thread e inicia ela para que outro peer possa fazer uma requisição de download
	public void startListening(String peerDirectory) throws IOException {
		
		// Cria o mecanismo de escuta e recebimento de requisições na porta informada pelo usuário
		ServerSocket serverSocket = new ServerSocket(peerListeningPort);
		
		// Utiliza a classe Executors para criar um objeto da interface ExecutorService. Com o método newSingleThreadExecutor(), uma thread é aberta para realizar as tarefas submetidas de forma assíncrona
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		
		// Usa o método subit() para enviar uma tarefa ao objeto/thread criada.
		executorService.submit(() -> {
			
			// Utiliza o loop para escutar indefinidamente requisições de outros peers
			while (true) {

				// Espera pelo ingresso de uma conexão e cria um novo socket
				Socket clientSocket = serverSocket.accept();

				// Cria um objeto da classe aninhada utilizando as informações do requisitante e inicia de fato a execução da thread. O método run() é chamado após o método start()
				PortListenerThread listenerThread = new PortListenerThread(clientSocket, peerDirectory);
				listenerThread.start();
			}
		});
		
		//serverSocket.close();
	}

	// Recebe os dados da thread de escuta e faz o envio do arquivo pedido
	public void receiveDownloadRequest(Socket socket, String serverDirectory) throws IOException {
		
		// Faz a leitura do fluxo de dados vindo do socket do peer cliente e armazena o nome do arquivo desejado
		DataInputStream reader = new DataInputStream(socket.getInputStream());
		String fileName = reader.readUTF();
		
		// Cria um objeto File contendo o arquivo desejado pelo peer cliente
		File file = new File(serverDirectory, fileName);
		
		// Cria um objeto para escrever dados no fluxo de saída
		DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
		
		// Cria um FileInputStream com o arquivo desejado em bytes
		FileInputStream fileInputStream = new FileInputStream(file);

		// Cria um buffer (string de bytes), grava os bytes lidos do arquivo e envia para o fluxo de saída
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = fileInputStream.read(buffer)) != -1) {
			writer.write(buffer, 0, bytesRead);
		}

		reader.close();
		writer.close();
		fileInputStream.close();
	}
	
	// Envia uma requisição de conexão TCP para o IP e porta do peer que vai agir como servidor e enviar o arquivo
	public void sendDownloadRequest(String serverIP, int serverListeningPort, String fileName, String clientDirectory) throws UnknownHostException, IOException {

			// Tenta criar uma conexão com o host remoto com base nas informações passadas
			Socket socket = new Socket(serverIP, serverListeningPort);
			
			// Faz o envio do nome do arquivo no fluxo de dados 
			DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
			writer.writeUTF(fileName);
			
			// Cria um objeto para ler os dados vindos do peer servidor
			InputStream inputStream = socket.getInputStream();
			
			// Cria um FileOutputStream para escrever os bytes em um arquivo no diretório do peer cliente
			FileOutputStream fileOutputStream = new FileOutputStream(clientDirectory+"\\"+fileName);
			
			// Cria um buffer (string de bytes), grava os bytes lidos no socket e grava no arquivo
			byte[] buffer = new byte[1024];
			int bytesRead;

			while((bytesRead = inputStream.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}

			writer.close();
			inputStream.close();
			fileOutputStream.close();
			socket.close();
	}

	// Faz a leitura dos nomes dos arquivos contidos no diretório informado
	public static List<File> listFiles(String peerDirectory){

		List<File> files = new ArrayList<>();
		File directory = new File(peerDirectory);
		File[] fileList = directory.listFiles();
		if (fileList != null)
			for (File file : fileList) 
				if (file.isFile())
					files.add(file);
		return files;
	}
	
	// Chama o método join do objeto remoto, que adiciona o peer à estrutura de dados do servidor, e mostra uma mensagem em casa de sucesso
	public static void menuJoin(Peer peer, Request stub) throws RemoteException {

		String response = stub.join(peer);
		if (response.equals("JOIN_OK"))
			System.out.println("\nSou peer " + peer.peerIP + ":" + peer.peerListeningPort + " com arquivos " + listFiles(peer.peerDirectory).stream().map(File::getName).collect(Collectors.joining(" ")));
	}
	
	// Faz a captura do nome do arquivo e busca no servidor os peers que o possuem. Caso nenhum peer possua o arquivo, devolve uma lista em branco. Armazena o nome do arquivo para ser usado na opção de download.
	public static void menuSearch(Peer peer, Request stub) throws RemoteException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.print("\nArquivo: ");
		peer.searchedFile = scanner.nextLine();
		//scanner.close();
		peer.searchedPeers = stub.search(peer, peer.searchedFile);
		if (peer.searchedPeers != null) {
			System.out.println("\nPeers com arquivo solicitado: ");
			//int i = 1;
			for (String peerInfo : peer.searchedPeers) {
				//System.out.println(i+". "+peerInfo);
				//i++;
				System.out.println(peerInfo);
			}
		}
	}

	// OK Verifica se há uma lista válida de peers com o arquivo desejado e, caso isso seja verdadeiro, chama o método para enviar uma requisição ao peer e, após o download, atualiza a estrutura de dados do servidor utlizando o objeto remoto. Em caso negativo, é pedido que seja feita uma nova busca.
	public static void menuDownload(Peer peer, Request stub) throws NumberFormatException, UnknownHostException, IOException {

		// Cria uma variável randômica e uma condição para simular a recusa de uma requisição de download
		Random random = new Random();
		
		if (peer.searchedPeers != null && !peer.searchedPeers.isEmpty()) {
			
			// Permite escolher de qual peer será feito o download
			/*
			//System.out.print("\nEscolha um peer para baixar " + peer.searchedFile + ": ");
			//Scanner scanner = new Scanner(System.in);
			//String[] serverInfo = peer.searchedPeers.get(scanner.nextInt()-1).split(":");
			*/
			
			// Escolhe aleatoriamente para qual peer enviar a solicitação
			String[] serverInfo = peer.searchedPeers.get(random.nextInt(peer.searchedPeers.size())).split(":");
			
			//scanner.close();
			
			// Em uma situação real, a recusa não seria feita neste ponto, mas sim pelo peer servidor
			if (random.nextBoolean()) {
				System.out.println("\nDownloading...");
				peer.sendDownloadRequest(serverInfo[0], Integer.parseInt(serverInfo[1]), peer.searchedFile, peer.peerDirectory);
				peer.peerFiles = listFiles(peer.peerDirectory);
				String responseU = stub.update(peer);
				if (responseU.equals("UPDATE_OK"))
					System.out.println("\nDownload concluído.");
			}
			else
				System.out.println("\nEnvio recusado.");
		}
		else {
			System.out.println("\nBusque por um arquivo válido.");
		}
		//else {
		//	System.out.println("\nArquivo não encontrado. Faça uma nova busca.");
		//}
		
	}

	// MAIN
	public static void main(String[] args) throws Exception{
		
		System.out.print("\033[H\033[2J");  
		System.out.flush();
		System.out.println("=== PEER ===");
		
		Scanner scanner = new Scanner(System.in);

		// Faz a captura do IP do peer
		System.out.print("\nIP: ");
		String peerIP = scanner.nextLine();

		// Faz a captura da porta de escuta do peer
		System.out.print("\nPorta de escuta: ");
		int peerListeningPort = scanner.nextInt();
		scanner.nextLine(); // Limpa o buffer

		// Faz a captura do diretório do peer onde se encontram os arquivos a serem compartilhados e onde os arquivos baixados serão salvos
		System.out.print("\nDiretório: ");
		String peerDirectory = scanner.nextLine();
		while (!new File(peerDirectory).isDirectory()) {
			System.out.println("Diretório inválido.");
			System.out.print("\nDiretório: ");
			peerDirectory = scanner.nextLine();
		}

		// Cria o objeto Peer com as informações capturadas
		Peer peer = new Peer(peerIP, peerListeningPort, peerDirectory, listFiles(peerDirectory));

		// Inicia uma thread de escuta na porta informada
		peer.startListening(peerDirectory);

		// Obtém o resgistro RMI
		Registry registry = LocateRegistry.getRegistry();

		// Obtém o objeto remoto do servidor buscando pelo nome
		Request stub = (Request) registry.lookup("rmi://127.0.0.1/request");

		boolean exit = false; // Cria a condição de parada do menu interativo
		
		// Apresenta o menu interativo
		System.out.println("\n=== MENU ===");
		System.out.println("1. JOIN");
		System.out.println("2. SEARCH");
		System.out.println("3. DOWNLOAD");
		//System.out.println("4. SAIR");
		System.out.println("============");
		
		while (!exit) {
			
			System.out.print("\nE: ");
			
			int choice = scanner.nextInt();
			scanner.nextLine(); // Limpa o buffer

			switch (choice) {
			case 1:
				menuJoin(peer, stub); 
				break;
			case 2:
				menuSearch(peer, stub);
				break;
			case 3:
				menuDownload(peer, stub);
				break;
			//case 4:
			//	exit = true;
			//	System.out.println("\nSaindo...");
			//	break;
			default:
				System.out.println("\nOpção inválida. Tente novamente.");
				break;
			}
		}
		
		//scanner.close();
	}
}