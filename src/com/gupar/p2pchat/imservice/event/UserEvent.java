package com.gupar.p2pchat.imservice.event;


public class UserEvent {
	private Event event;
	
	public enum Event {
		NONE,
		USER_UPDATE,
	}
	public UserEvent(Event event) {
		this.event = event;
	}
		
	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}
}
