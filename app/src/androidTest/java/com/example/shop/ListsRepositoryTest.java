package com.example.shop;

import android.support.test.runner.AndroidJUnit4;
import com.example.shop.app.AppSingleton;
import com.example.shop.rest.DataManager;
import com.example.shop.rest.RestRepository;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.prefs.Preferences;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;


@RunWith(AndroidJUnit4.class)
public class ListsRepositoryTest {
//    private InsalesAPI api;
    private Preferences preferences;
    private DataManager dataManager;

    @Before
    public void setUp(){
//        api = RestRepository.getInstance();
        preferences = Preferences.getInstance();
        dataManager = new DataManager(RestRepository.Companion.getINSTANCE(), AppSingleton.Companion.getINSTANCE().databaseHelper, preferences);
    }


    @Test
    public void testCashiersList(){
        TestObserver<User.Set> testObserver = new TestObserver<User.Set>();

        RestRepository.Companion.getINSTANCE()
                .getCashier(preferences.getCashboxToken())
                .subscribe(testObserver);

        testObserver.assertNoErrors();

        assertTrue(!testObserver.values().isEmpty());
        testObserver.assertComplete();

        // Data
        final User.Set data = testObserver.values().get(0);
        assertNotNull(data);
        assertNotNull(data.getItems());
    }


    @Test
    public void testHistory(){
        TestObserver<OrderToPost.Set> testObserver = new TestObserver<>();

        RestRepository.Companion.getINSTANCE()
                .getHistory(preferences.getHistoryLastId(),
                        preferences.getHistoryLastDate(),
                        OrderToPost.LIMIT, false, preferences.getCashboxToken())
                .subscribe(testObserver);

        testObserver.assertNoErrors();

        assertTrue(!testObserver.values().isEmpty());
        testObserver.assertComplete();

        // Data
        final OrderToPost.Set data = testObserver.values().get(0);
        assertNotNull(data);
        assertNotNull(data.getItems());
    }


    @Test
    public void testCostItems(){
        TestObserver<CostItem.Set> testObserver = new TestObserver<>();

        RestRepository.Companion.getINSTANCE().getCostItems(preferences.getPrefsLastIdCostItems(),
                preferences.getPrefsLastSavedDateCostItems(),
                CostItem.getLimit(),preferences.getCashboxToken())
                .subscribe(testObserver);

        testObserver.assertNoErrors();

        assertTrue(!testObserver.values().isEmpty());
        testObserver.assertComplete();

        // Data
        final CostItem.Set data = testObserver.values().get(0);
        assertNotNull(data);
        assertNotNull(data.getItems());
    }

    @Test
    public void testSellingPointInfo(){
        TestObserver<SellingPointInfo> testObserver = new TestObserver<>();

        RestRepository.Companion.getINSTANCE().getSellingPointInfo(preferences.getCashboxToken(), preferences.getIsApkFromMarket(), BuildConfig.VERSION_NAME, null, null)
                .subscribe(testObserver);

        testObserver.assertNoErrors();

        assertTrue(!testObserver.values().isEmpty());
        testObserver.assertComplete();

        // Data
        final SellingPointInfo data = testObserver.values().get(0);
        assertNotNull(data);
    }

   
    @Test
    public void testCashiersEmptyToken(){
        TestObserver<User.Set> testObserver = new TestObserver<>();

        RestRepository.Companion.getINSTANCE()
                .getCashier("")
                .subscribe(testObserver);

        //Request
        testObserver.assertTerminated();
        testObserver.assertError(AccessException.class);

        assertEquals(true,  testObserver.errors().get(0) instanceof AccessException);
        testObserver.assertNotComplete();
    }


    @Test
    public void testCategories(){
        final TestObserver<Category.Set> subscriber = new TestObserver<>();

        RestRepository.Companion.getINSTANCE()
                .getPagedCategories(preferences.getPrefsLastIdCategory(),
                        preferences.getPrefsLastSavedDateCategory(),500,
                        Preferences.getInstance().getCashboxToken())
                .subscribe(subscriber);

        //Request
        subscriber.assertNoErrors();
        assertTrue(!subscriber.values().isEmpty());
        subscriber.assertComplete();

        // Data
        final Category.Set data = subscriber.values().get(0);
        assertNotNull(data);
        assertNotNull(data.getItems());
    }
}