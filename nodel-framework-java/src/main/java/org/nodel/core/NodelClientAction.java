package org.nodel.core;

/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

import java.util.concurrent.atomic.AtomicReference;

import org.nodel.Handler;
import org.nodel.SimpleName;
import org.nodel.Strings;
import org.nodel.reflection.Serialisation;
import org.nodel.reflection.Value;

/**
 * Represents a Nodel client action dependency.
 */
public class NodelClientAction {
    
    /**
     * Released or not.
     */
    @Value(name = "closed")
    private boolean _closed = false;
    
    /**
     * (see public)
     */
    @Value(name = "isUnbound")
    private boolean _isUnbound = false;
    
    /**
     * Whether or not this is unbound.
     */
    public boolean isUnbound() {
        return _isUnbound;
    }

    /**
     * The node name.
     */
    @Value(name = "node")
    protected SimpleName _node;

    /**
     * The node event
     */
    @Value(name = "action")
    protected SimpleName _action;

    /**
     * The composite nodel action point
     */
    @Value(name = "nodelPoint")
    protected NodelPoint _nodelPoint;

    /**
     * Used for monitoring purposes.
     */
    private Handler.H1<Object> _monitor;
    
    /**
     * When wired status changes.
     */
    private Handler.H1<BindingState> wiredStatusHandler;
    
    /**
     * The last status.
     */
    @Value
    private AtomicReference<BindingState> _lastStatus = new AtomicReference<BindingState>(BindingState.Empty);    
    
    /**
     * Constructs a new Nodel Client to manage a single remote node.
     * (allows name and action to be null or empty)
     */
    public NodelClientAction(String name, String action) {
        if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(action))
            _isUnbound = true;

        if (Strings.isNullOrEmpty(name))
            name = "unbound";
        _node = new SimpleName(name);
        
        if (Strings.isNullOrEmpty(action))
            action = "unbound";
        _action = new SimpleName(action);
        
        _nodelPoint = NodelPoint.create(_node, _action);
    }

    /**
     * Returns the Node name being managed.
     */
    public SimpleName getNode() {
        return _node;
    }

    /**
     * Returns the Node action being managed.
     */
    public SimpleName getAction() {
        return _action;
    }

    /**
     * Returns the composite Nodel point.
     */
    public NodelPoint getNodelPoint() {
        return _nodelPoint;
    }

    /**
     * Registers interest in a Node's actions ("___ called", "___ completed" and
     * "___ failed" event interest are NOT automatically included).
     */
    public void registerActionInterest() {
        if (_isUnbound)
            return;
        
        NodelClients.instance().registerActionInterest(this);
    }

    /**
     * Calls an action on a remote node.
     */
    public void call(Object arg) {
        call0(arg);
    }
    
    /**
     * Calls an action on a remote node with default argument (null)
     */
    public void call() {
        call0(null);
    }    
    
    /**
     * (internal use)
     */
    private void call0(Object arg) {
        if (_monitor != null)
            _monitor.handle(arg);

        if (_isUnbound)
            return;

        NodelClients.instance().call(this, arg);
    } // (method)

    /**
     * Attaches a monitor.
     */
    public void attachMonitor(Handler.H1<Object> monitor) {
        if (monitor == null)
            throw new IllegalArgumentException("Cannot detach null monitor.");

        _monitor = monitor;
    }

    /**
     * Releases this binding,
     */
    public void close() {
        if (_closed)
            return;

        _closed = true;

        if (_isUnbound)
            return;
        
        NodelClients.instance().release(this);
    }

    public void attachWiredStatusChanged(Handler.H1<BindingState> handler) {
        if (handler == null)
            throw new IllegalArgumentException("Handler cannot be null.");
        
        this.wiredStatusHandler = handler; 
    } // (method)
    
    void setWiredStatus(BindingState status) {
        BindingState last = _lastStatus.getAndSet(status);
        
        if (last != status && this.wiredStatusHandler != null) {
            this.wiredStatusHandler.handle(status);
        }
    }

    @Override
    public String toString() {
        return Serialisation.serialise(this);
    }    

} // (class)