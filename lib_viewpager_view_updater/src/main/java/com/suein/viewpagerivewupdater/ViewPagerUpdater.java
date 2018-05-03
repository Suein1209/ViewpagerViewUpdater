package com.suein.viewpagerivewupdater;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.suein.viewpagerivewupdater.comm.ViewPagerViewUpdaterFragmentBase;

import org.greenrobot.eventbus.Subscribe;

import java.util.Hashtable;

import utils.v4.FragmentPagerItemAdapter;

public class ViewPagerUpdater {
    private volatile static ViewPagerUpdater instance;

    public static ViewPagerUpdater getInstance() {
        if (instance == null) {
            synchronized (ViewPagerUpdater.class) {
                if (instance == null) {
                    instance = new ViewPagerUpdater();
                }
            }
        }
        return instance;
    }

    private ViewPagerUpdater() {
    }

    private long updateTime = -1;

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    @Subscribe
    public void onSelectedEvent(ViewPagerPositionEvent event) {
        int itemIndex = event.scrollState == ViewPagerPositionEvent.ScrollState.SELECTED ? event.currentPage : event.nextPage;
        Fragment fragment = ((FragmentPagerItemAdapter) this.viewPager.getAdapter()).getPage(itemIndex);
        if (fragment instanceof ViewPagerViewUpdaterFragmentBase){
            final ViewPagerViewUpdaterFragmentBase csPartnerMainViewFragmentBase = (ViewPagerViewUpdaterFragmentBase) ((FragmentPagerItemAdapter) this.viewPager.getAdapter()).getPage(itemIndex);

//            if (isNeedUpdate(itemIndex) && (!isExistWithoutPage(itemIndex)) && (csPartnerMainViewFragmentBase.isFirstUpdatedView())) {
                if (((updateTime != -1) && isNeedUpdate(itemIndex)) || (csPartnerMainViewFragmentBase.isFirstUpdatedView())) {
                csPartnerMainViewFragmentBase.setFirstUpdatedView(false);
                doUpdateCurrnetTime(itemIndex);
                csPartnerMainViewFragmentBase.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        csPartnerMainViewFragmentBase.onUpdate();
                    }
                });
            }
        }
    }

    private void doUpdateCurrnetTime(int itemIndex) {
        upateTimeMap.put(itemIndex, System.currentTimeMillis());
    }

    private final Hashtable<Integer, Long> upateTimeMap = new Hashtable<Integer, Long>();

    private boolean isNeedUpdate(int itemIndex) {
        if (upateTimeMap.containsKey(itemIndex)) {
            long newUpdateTime = System.currentTimeMillis();
            long oldUpdateTime = upateTimeMap.get(itemIndex);
            return ((newUpdateTime - oldUpdateTime) > updateTime);
        }

        return true;
    }


    private int[] withoutPages;

    public void setWithoutPage(int... withoutPages) {
        this.withoutPages = withoutPages;
    }

    private boolean isExistWithoutPage(int pageNum) {
        if (withoutPages == null) return false;
        for (int page : withoutPages) {
            if (pageNum == page) {
                return true;
            }
        }

        return false;
    }

    private ViewPager viewPager;

    public void setViewPager(ViewPager viewPager) {
        this.viewPager = viewPager;
        this.viewPager.removeOnPageChangeListener(viewPagerOnChangeListener);
        this.viewPager.addOnPageChangeListener(viewPagerOnChangeListener);
    }

    private final ViewPager.OnPageChangeListener viewPagerOnChangeListener = new ViewPager.OnPageChangeListener() {

        private int scrollState;
        private boolean isPostEvent = false;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            //좌우 드래그의 상태를 보고 다음에 보여질 fragment가 어떤 것인지 전달한다
            if (ViewPager.SCROLL_STATE_DRAGGING == scrollState && (!isPostEvent)) {
                if (positionOffset == 0 && positionOffsetPixels == 0) {
                    return;
                }

                ViewPagerPositionEvent viewPagerPositionEvent = null;
                int currentPage = viewPager.getCurrentItem();
                if (positionOffset >= 0.5) {
                    viewPagerPositionEvent = new ViewPagerPositionEvent(ViewPagerPositionEvent.ScrollState.SCROLLING, currentPage, currentPage - 1);

                } else if (positionOffset < 0.5) {
                    viewPagerPositionEvent = new ViewPagerPositionEvent(ViewPagerPositionEvent.ScrollState.SCROLLING, currentPage, currentPage + 1);

                }

                if (viewPagerPositionEvent != null) {
                    onSelectedEvent(viewPagerPositionEvent);
                }

                isPostEvent = true;
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_SETTLING) {
                isPostEvent = false;
            }
            scrollState = state;
        }

        @Override
        public void onPageSelected(int position) {
            if (!isPostEvent) {
                ViewPagerPositionEvent viewPagerPositionEvent = new ViewPagerPositionEvent(ViewPagerPositionEvent.ScrollState.SELECTED, position);
                onSelectedEvent(viewPagerPositionEvent);
            }
            isPostEvent = false;
        }

    };
}
