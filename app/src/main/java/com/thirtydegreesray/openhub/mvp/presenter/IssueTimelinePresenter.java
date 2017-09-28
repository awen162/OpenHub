package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IIssueTimelineContract;
import com.thirtydegreesray.openhub.mvp.model.Issue;
import com.thirtydegreesray.openhub.mvp.model.IssueEvent;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Created by ThirtyDegreesRay on 2017/9/27 11:54:43
 */

public class IssueTimelinePresenter extends BasePresenter<IIssueTimelineContract.View>
        implements IIssueTimelineContract.Presenter {

    @AutoAccess Issue issue;
    private ArrayList<IssueEvent> timeline;
    private ArrayList<IssueEvent> events;

    @Inject
    public IssueTimelinePresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadTimeline(1, false);
    }


    @Override
    public void loadTimeline(int page, boolean isReload) {
//        if(page == 1)
//            loadEvents();
        loadComments(page, isReload);
    }

    private void loadComments(final int page, final boolean isReload){
        mView.showLoading();
        final boolean readCacheFirst = page == 1 && !isReload;
        HttpObserver<ArrayList<IssueEvent>> httpObserver
                = new HttpObserver<ArrayList<IssueEvent>>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                handleError(error);
            }

            @Override
            public void onSuccess(HttpResponse<ArrayList<IssueEvent>> response) {
                mView.hideLoading();
                if (isReload || timeline == null || readCacheFirst) {
                    timeline = response.body();
                    timeline.add(0, getFirstComment());
                } else {
                    timeline.addAll(response.body());
                }
                if(response.body().size() == 0 && timeline.size() != 0){
                    mView.setCanLoadMore(false);
                } else {
                    mView.showTimeline(timeline);
                }
            }
        };
        generalRxHttpExecute(new IObservableCreator<ArrayList<IssueEvent>>() {
            @Override
            public Observable<Response<ArrayList<IssueEvent>>> createObservable(boolean forceNetWork) {
                return getIssueService().getIssueComments(forceNetWork, issue.getRepoAuthorName(),
                        issue.getRepoName(), issue.getNumber(), page);
            }
        }, httpObserver, readCacheFirst);
    }

    private void loadEvents() {
        HttpObserver<ArrayList<IssueEvent>> httpObserver
                = new HttpObserver<ArrayList<IssueEvent>>() {
            @Override
            public void onError(Throwable error) {

            }

            @Override
            public void onSuccess(HttpResponse<ArrayList<IssueEvent>> response) {
                events = filterEvents(response.body());
            }
        };
        generalRxHttpExecute(new IObservableCreator<ArrayList<IssueEvent>>() {
            @Override
            public Observable<Response<ArrayList<IssueEvent>>> createObservable(boolean forceNetWork) {
                return getIssueService().getIssueEvents(forceNetWork, issue.getRepoAuthorName(),
                        issue.getRepoName(), issue.getNumber(), 1);
            }
        }, httpObserver);
    }

    private ArrayList<IssueEvent> filterEvents(ArrayList<IssueEvent> oriEvents){
        ArrayList<IssueEvent> filteredEvents = new ArrayList<>();
        if(oriEvents == null || oriEvents.size() == 0) return filteredEvents;
        for(IssueEvent event : oriEvents){
            if(event.getType() != null)
                filteredEvents.add(event);
        }
        return filteredEvents;
    }

    private IssueEvent getFirstComment(){
        IssueEvent firstComment = new IssueEvent();
        firstComment.setBodyHtml(issue.getBodyHtml());
        firstComment.setUser(issue.getUser());
        firstComment.setCreatedAt(issue.getCreatedAt());
        firstComment.setHtmlUrl(issue.getHtmlUrl());
        return firstComment;
    }

    private void handleError(Throwable error){
        if(!StringUtils.isBlankList(timeline)){
            mView.showErrorToast(getErrorTip(error));
        } else if(error instanceof HttpPageNoFoundError){
            mView.showTimeline(new ArrayList<IssueEvent>());
        } else {
            mView.showLoadError(getErrorTip(error));
        }
    }

}