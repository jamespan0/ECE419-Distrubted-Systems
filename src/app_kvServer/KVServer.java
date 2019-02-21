package app_kvServer;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import ecs.IECSNode;



import logger.LogSetup;

import org.apache.log4j.*;

import client.KVCommInterface;
import client.KVStore;

import shared.messages.TextMessage;

public class KVServer implements IKVServer, Runnable {
	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate - in echoServer gave range 49152 to 65535
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */

	// private variables 
	private static Logger logger = Logger.getRootLogger();
	private ServerSocket serverSocket;
	private int port;
	private int cacheSize;
	private CacheStrategy cacheStrategy;

    /*     START OF DATA STRUCTURES FOR KEY VALUE STORAGE                */
    private String filename = "./persistantStorage.data";
    private String tempname = "./.temp.persistantStorage.data";
    private LinkedHashMap<String, String> cache_LRU = new LinkedHashMap<String, String>(getCacheSize(), .75f , true) { 
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) { 
            return size() > getCacheSize(); 
        } 
    }; 
    private LinkedHashMap <String, String> cache_FIFO = new LinkedHashMap<String, String>() { 
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) { 
            return size() > getCacheSize(); 
        } 
    }; 
    private LFUCache lfucache;
    private File outputFile = new File(filename);
    private File tempFile = new File(tempname);

    private BufferedWriter disk_write;
    private BufferedWriter temp_disk_write;
    private BufferedReader disk_read;


    /*     END OF DATA STRUCTURES FOR KEY VALUE STORAGE                */



    /*  M2 variables start  */

    private int m2_port;
    private int m2_cachesize;
    private StringBuffer stringBuffer;  //hash of tuple encrypted
    // int to store ports, string stores map
    private TreeMap <Integer, IECSNode> metadata = new TreeMap<Integer, IECSNode>();

	private boolean running = false;
	public boolean activated = false;
	public boolean writeLock = false;
//    private serverTypes serverStatus;
    private String servername;


/*
    public enum serverTypes {
        SERVER_WRITE_LOCK,    //do not process ECS nor client requests, server shut down
        SERVER_STOPPED, //block all client requests, only process ECS
        SERVER_    // all client and ECS requests are processed
    }
*/


    /*  M2 variables end */

    //M1 KVServer
	public KVServer(int port, int cacheSize, String strategy) {
        this.port = port;
        this.cacheSize = cacheSize;

        try {
            outputFile.createNewFile();
            BufferedWriter disk_write = new BufferedWriter(new FileWriter(outputFile,true)); 
            BufferedWriter temp_disk_write = new BufferedWriter(new FileWriter(tempFile,true)); 
        } catch (IOException ioe) {
            System.out.println("Trouble creating file: " + ioe.getMessage());
        }

        try {
            BufferedReader disk_read = new BufferedReader(new FileReader(outputFile)); 
        } catch (FileNotFoundException e) {
            //create file anew if file is not found
            outputFile = new File(filename);
            try {
                outputFile.createNewFile();
            } catch (IOException ioe) {
                System.out.println("Trouble creating file: " + ioe.getMessage());
            }
        }

        if (strategy.equals("FIFO")) {
            this.cacheStrategy = IKVServer.CacheStrategy.FIFO;
            System.out.println("Constructing cache_FIFO");

        }
        else if (strategy.equals("LFU")) {
            this.cacheStrategy = IKVServer.CacheStrategy.LFU;
            this.lfucache = new LFUCache(getCacheSize());
        }
        else if (strategy.equals("LRU")) {
            this.cacheStrategy = IKVServer.CacheStrategy.LRU;
        }
        else {
            this.cacheStrategy = IKVServer.CacheStrategy.FIFO; //in case of fail, just do FIFO operation
        }
        
	}

    //metadata is string
    //cacheSize is int
    //replacementstrategy is String
	public void initKVServer(IECSNode meta_data, int cacheSize, String strategy) {
    //need to figure out how to get metadata
        this.m2_cachesize = cacheSize;
//        serverStatus = serverTypes.SERVER_STOPPED;

        //function to add current storage servers to TreeMap metadata
        activated = false ;
        writeLock = false ;

/*
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(metadata.getBytes());
            byte[] messageDigestMD5 = messageDigest.digest();
            this.stringBuffer = new StringBuffer();
            for(byte bytes : messageDigestMD5) {
                this.stringBuffer.append(String.format("%02x",bytes & 0xff));
            } 
        } catch(NoSuchAlgorithmException exception) {
            exception.printStackTrace(); 
        }

        decrypt(stringBuffer);
*/

	}


	public void start() {
		activated = true;

		logger.info("Server activated on port: " 
					+ serverSocket.getLocalPort());
	}

	public void stop() {
		activated = false;

		logger.info("Server deactivated on port: " 
					+ serverSocket.getLocalPort());
	}

	public void shutDown() {
		clearCache();
		close();
	}

	public void lockWrite() {
		writeLock = true;

		logger.info("Server write lock enabled");
	}

	public void unLockWrite() {
		writeLock = false;

		logger.info("Server write lock disabled");
	}

	public void moveData(String[] range, String server) {

    //check if range array is proper
    if (range.length != 2) {
        //range of array not proper, return fail to ECS
        return;
    }
    // movehash gives integer in metadata for range of hashes selected for this server
    Integer moveHash; /* = ECSHASH(server);*/

    if (metadata.get(moveHash) == null) {
        //Server not allocated, return fail to ECS
        return;
    } else {
        

    }


	}

	public void update(IECSNode meta_data) {
        //need function to update map
        //use IECSNODE meta_data to update map
        


	}

    public LinkedHashMap<String, String> getFIFO() {
        return this.cache_FIFO ;
    }

    public void putFIFO(String key, String value) {
        this.cache_FIFO.put(key,value);
    }
    
	
	@Override
	public int getPort(){
		return serverSocket.getLocalPort();
	}

	@Override	
	public String getHostname(){
		InetAddress ip;
		String hostname;
		try {
			ip = InetAddress.getLocalHost();
			hostname = ip.getHostName();

			return hostname;
		} catch (UnknownHostException e) {
			e.printStackTrace();
	        
			logger.error("Error! " +
				"Unknown Host!. \n", e);
		
			return "Unknown Host";
		}
	}

	@Override
	public CacheStrategy getCacheStrategy(){
		return this.cacheStrategy;
	}

	@Override
	public int getCacheSize(){
		return this.cacheSize;
	}

	@Override
    public boolean inStorage(String key){

        // iterate through memory to see if located in disk
        String strCurrentLine;
        try {
            BufferedReader disk_read = new BufferedReader(new FileReader(outputFile)); 

            while ((strCurrentLine = disk_read.readLine()) != null) {
                String[] keyValue = strCurrentLine.split(" "); // keyValue[0] is the key
                if (keyValue[0].equals(key)) {
                    return true;
                }
            }
            disk_read.close();
        } catch (IOException e) {
			System.out.println("Error! unable to read/write!");
		} 
        // no key found in cache nor storage

		return false;
	}


	@Override
    public boolean inCache(String key){

        if (getCacheStrategy() == IKVServer.CacheStrategy.FIFO) {
            // FIFO case
            return this.cache_FIFO.containsKey(key);
        } else if (getCacheStrategy() == IKVServer.CacheStrategy.LRU) {
            // LRU case
            return cache_LRU.containsKey(key);
        } else {
            // LFU case
            return lfucache.lfu_containsKey(key);
        }
	}

	@Override
    public String getKV(String key) throws Exception{
        // Constraint checking for key and value
        if (key.getBytes("UTF-8").length > 20) {
            return "ERROR"; //ERROR due to key length too long
        }

        if (key.contains(" ")) {
            return "ERROR" ; //ERROR due to whitespace
        }
        

        if (inCache(key)) {
            if (getCacheStrategy() == IKVServer.CacheStrategy.FIFO) {
                // FIFO case
                String value = cache_FIFO.get(key);
                return value;
            } else if (getCacheStrategy() == IKVServer.CacheStrategy.LRU) {
                // LRU case
                String value = cache_LRU.get(key);
                return value;
            } else {
                // LFU case
                String value = lfucache.lfu_get(key);
                return value;
            }
        }
        // iterate through memory to see if located in disk
        String strCurrentLine;
        try {
            disk_read = new BufferedReader(new FileReader(outputFile)); //true so that any new data is just appended
            while ((strCurrentLine = disk_read.readLine()) != null) {
                String[] keyValue = strCurrentLine.split(" "); // keyValue[0] is the key
                if (keyValue[0].equals(key)) {
                    return keyValue[1];
                }
            }
            disk_read.close();
        } catch (IOException ioe) {
            System.out.println("Trouble Reading file: " + ioe.getMessage());
            outputFile.createNewFile();
        }

		return "ERROR_NO_KEY_FOUND";
	}

	@Override
    public String putKV(String key, String value) throws Exception{
        /*
            strategy:
            1) use map to store data structure
        */

        // Constraint checking for key and value
        if (key.contains(" ")) {
            return "ERROR"; //ERROR due to whitespace
        }

		String result = "";		

        if (key.getBytes("UTF-8").length > 20) {
            return "ERROR"; //ERROR due to key length too long
        }

        //120kB is around 122880 bytes
        if (value.getBytes("UTF-8").length > 122880) {
            return "ERROR"; //ERROR due to value length too long
        }


        if (value.equals("null")) {
			result = "ERROR"; //error if deleting non-existent key
            System.out.println("TEST GOES INTO VALUE EQUALS NULL");

            // delete key
            if (inCache(key)) {
                if (getCacheStrategy() == IKVServer.CacheStrategy.FIFO) {
                    // FIFO case
                    this.cache_FIFO.remove(key);
                } else if (getCacheStrategy() == IKVServer.CacheStrategy.LRU) {
                    // LRU case
                    cache_LRU.remove(key);
                } else {
                    // LFU case
                    lfucache.lfu_remove(key);
                }

				result = "UPDATE"; //delete successful
            }

            if (inStorage(key)){
                // need to remove the key from the list
                String strCurrentLine;
                try {
                    BufferedWriter temp_disk_write = new BufferedWriter(new FileWriter(tempFile,true)); //true so that any new data is just appended
                    BufferedReader disk_read = new BufferedReader(new FileReader(outputFile)); //true so that any new data is just appended

                    while ((strCurrentLine = disk_read.readLine()) != null) {
                        String[] keyValue = strCurrentLine.split(" "); // keyValue[0] is the key
                        if (!keyValue[0].equals(key)) {
                            temp_disk_write.write(strCurrentLine);
                        }
                    }
                    disk_read.close();
                    temp_disk_write.close();
                    // at end rename file
                    boolean success = tempFile.renameTo(outputFile); //renamed

					result = "UPDATE"; //delete successful
                } catch (IOException e) {
                    System.out.println("Error! unable to read!");
                } 

            }
        } else {

            // insert key in cache
            if (getCacheStrategy() == IKVServer.CacheStrategy.FIFO) {
                // FIFO case
                System.out.println("Inserting FIFO Value");
                if (this.cache_FIFO == null) {
                    System.out.println("ERROR: cache_FIFO is null");
                }
				if (this.cache_FIFO.containsKey(key)) {
					result = "UPDATE";
				} else {
					result = "SUCCESS";
				}
                this.cache_FIFO.put(key,value);
                System.out.println("After FIFO Value");
                // print stuff out

            } else if (getCacheStrategy() == IKVServer.CacheStrategy.LRU) {
                // LRU case
				if (cache_LRU.containsKey(key)) {
					result = "UPDATE";
				} else {
					result = "SUCCESS";
				}
                cache_LRU.put(key,value);
            } else {
                // LFU case
				if (lfucache.lfu_containsKey(key)) {
					result = "UPDATE";
				} else {
					result = "SUCCESS";
				}
                lfucache.lfu_put(key,value);
            }

            // insert key in storage


            if (inStorage(key)) {
                // if key already in storage check value
                String strCurrentLine;
                boolean change = false;

                try {
                    BufferedReader disk_read = new BufferedReader(new FileReader(outputFile)); 

                    while ((strCurrentLine = disk_read.readLine()) != null) {
                        String[] keyValueA = strCurrentLine.split(" "); // keyValue[0] is the key
                        if (keyValueA[0].equals(key)) {
                            if (!keyValueA[1].equals(value)) {
                                change = true;
                                System.out.println("CHANGE GOES TO TRUE");
                                //remove key from disk
                            }
                        }
                    }
                    disk_read.close();
                } catch (IOException e) {
			        System.out.println("Error! unable to read/write!");
		        }       

                if (change) {
                    try {
                        BufferedWriter temp_disk_write1 = new BufferedWriter(new FileWriter(tempFile,true)); 
                        BufferedReader disk_read1 = new BufferedReader(new FileReader(outputFile)); 

                        while ((strCurrentLine = disk_read1.readLine()) != null) {
                            String[] keyValue = strCurrentLine.split(" "); // keyValue[0] is the key
                            if (!keyValue[0].equals(key)) {
                                temp_disk_write1.write(strCurrentLine);
                            }
                        }
                        disk_read1.close();
                        temp_disk_write1.close();
                    // at end rename file
                        boolean success = tempFile.renameTo(outputFile); //renamed

//                                    result = "UPDATE"; //delete successful
                    } catch (IOException e) {
                        System.out.println("Error! unable to read!");
                    } 

                    BufferedWriter disk_write = new BufferedWriter(new FileWriter(outputFile,true)); //true so that any new data is just appended
                    String diskEntry = key + " " + value + "\n" ;
                    disk_write.write(diskEntry);
                    disk_write.close();
                }

            } else {
                BufferedWriter disk_write = new BufferedWriter(new FileWriter(outputFile,true)); //true so that any new data is just appended
                String diskEntry = key + " " + value + "\n" ;
                disk_write.write(diskEntry);
                disk_write.close();

            }

        }

		return result;


	}


	@Override
    public void clearCache(){
        if (getCacheStrategy() == IKVServer.CacheStrategy.FIFO) {
            // FIFO case
            cache_FIFO.clear();
        } else if (getCacheStrategy() == IKVServer.CacheStrategy.LRU) {
            // LRU case
            cache_LRU.clear();
        } else {
            // LFU case
            lfucache.lfu_clear();
        }

	}

	@Override
    public void clearStorage(){
            clearCache();        //clear the cache
            //delete memory file on disk
            try {
                outputFile.delete();
                outputFile.createNewFile();
            } catch (IOException ioe) {
                System.out.println("Trouble deleting/creating file: " + ioe.getMessage());
            }
	}

	private boolean initializeServer() {
		logger.info("Initializing server...");
		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server listening on port: " 
					+ serverSocket.getLocalPort());

			connectECS();

			return true;
		} catch (IOException e) {
			logger.error("Error! Cannot open server socket:");
			if(e instanceof BindException){
				logger.error("Port " + port + " is already bound!");
			}
			return false;
		}
	}

	public void connectECS() {
		try {
			logger.info("Waiting for ECS connection on port: " 
					+ serverSocket.getLocalPort());

			Socket ecs = serverSocket.accept();                
			ECSConnection connection = 
					new ECSConnection(this, ecs);
			new Thread(connection).start();
	               
			logger.info("Connected to ECS coordinator: " 
					+ ecs.getInetAddress().getHostName() 
					+  " on port " + ecs.getPort());

		} catch (IOException e) {
			logger.error("Error! Cannot open server socket:");
			if(e instanceof BindException){
				logger.error("Port " + port + " is already bound!");
			}
		
		}
	}

	@Override
	public void run(){
		running = initializeServer();

		if(serverSocket != null) {
			while(this.running){
				try {
					Socket client = serverSocket.accept();                
					ClientConnection connection = 
							new ClientConnection(this, client);
					new Thread(connection).start();
			           
					logger.info("Connected to client " 
							+ client.getInetAddress().getHostName() 
							+  " on port " + client.getPort());
				} catch (IOException e) {
					logger.error("Error! " +
							"Unable to establish connection. \n", e);
				}
			}
		}
		logger.info("Server stopped.");
	}

	@Override
	public void kill(){	
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + port, e);
		}
	}

	@Override
	public void close(){
		//add actions to free data structures, cache, etc

		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + port, e);
		}
	}

    /**
     * Main entry point for the server application. 
     * @param args contains the port number at args[0], cachesize at args[1], strategy at args[2].
     */
	public static void main(String[] args) {
    	try {
			new LogSetup("logs/server.log", Level.ALL);
			if(args.length != 3) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: Server <port> <cache size> <cache strategy>!");
			} else {
				int port = Integer.parseInt(args[0]);
				int cacheSize = Integer.parseInt(args[1]);
				String cacheStrategy = args[2];
				KVServer app = new KVServer(port, cacheSize, cacheStrategy);
				app.run();
			}
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException nfe) {
			System.out.println("Error! Invalid argument(s)!");
			System.out.println("Usage: Server <port> <cache size> <cache strategy>!");
			System.exit(1);
		}
    }
}
