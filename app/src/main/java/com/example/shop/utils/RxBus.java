package com.example.shop.utils;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 22.03.17
 * Time: 16:49
 */

public class RxBus {

    private static RxBus instance;

    private PublishSubject<Object> subject = PublishSubject.create();

    public static RxBus instanceOf() {
        if (instance == null) {
            instance = new RxBus();
        }
        return instance;
    }

    /**
     * Pass any event down to event listeners.
     */
    public void dispatchEvent(Object object) {
        subject.onNext(object);
    }

    /**
     * Subscribe to this Observable. On event, do something
     * e.g. replace a fragment
     */
    public Observable<Object> getEvents() {
        return subject;
    }}