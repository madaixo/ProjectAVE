package com.grapeshot.halfnes.network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CircularQueue<T> {

	private BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
	private int capacity;
	
	public CircularQueue(int capacity){
		this.capacity = capacity;
	}
	
	public void offer(T item){
		if(queue.size() >= capacity)
			queue.poll();
		queue.offer(item);
	}
	
	public T take() throws InterruptedException{
		return this.queue.take();
	}
	
	public int size(){
		return this.queue.size();
	}
}