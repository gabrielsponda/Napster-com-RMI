package model;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import peer.Peer;

public interface Request extends Remote{

	public String join(Peer peer) throws RemoteException;
	public List<String> search(Peer peer, String fileName) throws RemoteException;
	public String update(Peer peer) throws RemoteException;
}
