package lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import lib.Chunk;

public class P2pFile {
	FileMetaData metadata;
	ArrayList<Chunk> chunkList;
	
	public P2pFile(String fileName) {
		File f = new File(fileName);
		int numChunks = (int)Math.ceil(f.length()*1.0/Chunk.CHUNK_SIZE);
		metadata = new FileMetaData(fileName, numChunks);
		chunkList = new ArrayList<>(numChunks);
		for(int chunkNumber=0; chunkNumber<numChunks; chunkNumber++){
			Chunk newChunk = getChunk(chunkNumber);
			chunkList.add(newChunk);
			metadata.setChunkNameOf(chunkNumber, newChunk.chunkName);
		}
	}
	
	public P2pFile(FileMetaData oldMetaData, ArrayList<Chunk> downladedChunkList) {
		this.metadata = oldMetaData;
		this.chunkList = downladedChunkList;	
	}
	
	ArrayList<Chunk> getChunkList() {
		return this.chunkList;
	}
	
	FileMetaData getMetadata() {
		return this.metadata;
	}
	
	Chunk getChunk(int chunkNumber){
		try {
			byte[] b = new byte[Chunk.CHUNK_SIZE];
			RandomAccessFile raf = new RandomAccessFile(metadata.fileName, "r");// doesn't have to be done every time!
			//System.out.println(raf.length());
			if(raf.length()<=chunkNumber*256){
				raf.close();
				throw new Exception("Required chunk number "+chunkNumber+" does not exist in "+metadata.fileName);
			}
			raf.seek(chunkNumber*256);
			raf.read(b);
			//System.out.println(Arrays.toString(b).replace("[", "").replace("]", "").replace(", ", newChar));
			raf.close();
			return new Chunk(Integer.toString(b.hashCode()), b);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String toString() {
		return "Metadata:"+this.metadata+" / Chunks:"+this.chunkList;
	}
	
	public static void main(String[] args) {
		System.out.println(new P2pFile("types"));
	}
}