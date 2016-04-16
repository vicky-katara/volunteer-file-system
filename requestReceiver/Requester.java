package requestReceiver;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Iterator;

import lib.SenderReceiver;
import lib.Chunk;
import lib.FileMetaData;
import lib.P2pFile;
import lib.Packet;
import lib.Peer;


public class Requester extends Thread{
	
	private DatagramPacket udpDatagram;
	private int option;
	private String chunkname;
	private byte[] storeBytes;
	
	private FileMetaData metadata;
	private ArrayList<Chunk> chunkList;
	private String[] splitted;
	//private ArrayList<ChunkTriplePeer> chunkPeerList;
	
	//Refer types 100, 101
	public boolean checkIfPeerIsUp(Peer p){
		new SenderReceiver().sendDatagramAndGetUDPReplyOn(p, new Packet(100, "").getPayload());
		Packet receivedPacket = new Packet(new String(udpDatagram.getData()));
		if(receivedPacket.getOption()==101) {
			return true;
		}
		return false;
	}
	

	//Refer type 102, 103
	public void getChunkList(String chunkname, Peer p, ArrayList<Peer> availablePeers) throws Exception{
		new SenderReceiver().sendDatagramAndGetUDPReplyOn(p, new Packet(102,chunkname).getPayload());
		Packet receivedPacket = new Packet(new String(udpDatagram.getData()));
		if(receivedPacket.getOption()==103) {
			splitted = receivedPacket.getData().split(":");
			chunkname = splitted[0];
			Chunk toDistribute = new Chunk(chunkname, splitted[1]);
			byte[] chunksRetrieved = toDistribute.returnBytes();
			
			RoundRobin<Peer> p4 = new RoundRobin<Peer>(availablePeers);
			Iterator it = p4.iterator();
			int chunkIndex = 0;
			while(chunkIndex != chunksRetrieved.length-1) {
				//for each chunk, find peer using round robin
				//send store request to that peer
				Peer toSend = (Peer) it.next();
				if(checkIfPeerIsUp(toSend)){
					//100, 101 - passed
					chunkIndex++;
					//104, 105
					Chunk c = new Chunk(chunkname,String.valueOf(chunksRetrieved[chunkIndex]));
					push(toSend, c);
				} else {
					////100, 101 - failed
					System.out.println(toSend.getIpAddress()+"is down!");
				}	
			}	
		} else {
			throw new Exception("ACK not received");
		}
		
	}
		
	
	//Refer types 104, 105
	public void push(Peer p, Chunk c)throws Exception{
		new SenderReceiver().sendDatagramAndGetUDPReplyOn(p, new Packet(104,c.getChunkName()+":"+c.returnBytes()).getPayload());
		Packet receivedPacket = new Packet(new String(udpDatagram.getData()));
		if(receivedPacket.getOption()==105 && receivedPacket.getData().split(":")[0].equals(c.getChunkName())) {
			System.out.println("Chunk inserted");
			//Update metadata here
		} else {
			throw new Exception("Reply not expected");
		}
	}
	
	
	//Refer types 106, 107
	public void delete(Peer p, Chunk c) throws Exception{
		new SenderReceiver().sendDatagramAndGetUDPReplyOn(p, new Packet(106,c.getChunkName()).getPayload());
		Packet receivedPacket = new Packet(new String(udpDatagram.getData()));
		if(receivedPacket.getOption()==107 && receivedPacket.getData().split(":")[0].equals(c.getChunkName())) {
			System.out.println("Chunk deleted");
		} else {
			throw new Exception("Reply not expected");
		}
		
	}
	
	public void populate(P2pFile p2p){
		chunkList = p2p.getChunkList();
		metadata = p2p.getMetadata();
	}
	
	public static void main(String[] args){
		
	}
}
	
	class RoundRobin<Peer> implements Iterable<Peer>{
		private ArrayList<Peer> peers = new ArrayList<Peer>();
		
		public RoundRobin(ArrayList<Peer> peers){
			this.peers = peers;
		}
		@Override
		public Iterator<Peer> iterator() {
			return new Iterator<Peer>(){
				private int index = 0;
				
				public Peer next(){
					Peer curr = peers.get(index);
					//System.out.println(index);
					index = (index+1)%peers.size();
					return curr;
				}
				@Override
				public boolean hasNext() {
					// TODO Auto-generated method stub
					return true;
				}
			};
		}
		
	}


