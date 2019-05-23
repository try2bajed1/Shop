package com.example.shop;

import android.support.test.runner.AndroidJUnit4;
import com.example.shop.app.AppSingleton;
import com.example.shop.model.BDUtilsKt;
import com.example.shop.model.KOrder;
import com.example.shop.model.OrdersModel;
import com.example.shop.rest.DataManager;
import com.example.shop.rest.RestRepository;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.prefs.Preferences;


@RunWith(AndroidJUnit4.class)
public class PostPurchaseCaseTest {
    private RestRepository restRepository;
    private Preferences preferences;
    private DataManager dataManager;

    @Before
    public void setUp(){
        preferences = Preferences.getInstance();
        restRepository = RestRepository.Companion.getINSTANCE();

        OrdersModel.Companion.getInstance().resetCurrentOrder();
        KOrder order = OrdersModel.Companion.getInstance().getKOrder();
        Variant variant = getVariant(1, 1000, 1);
        variant.setTitle("title");
        order.add(variant, BigDecimal.ONE);

        new PostPurchaseCase(restRepository, preferences, AppSingleton.Companion.getINSTANCE().databaseHelper)
                .getPostObs(new PaymentResult(true,"","","", BDUtilsKt.toCopecks(OrdersModel.Companion.getInstance().getKOrder().getTotalPrice())));
    }


    private Variant getVariant(int id,long variantPrice, int quantity) {
        Variant variant = new Variant();
        variant.setId(id);
        variant.setQuantity(quantity);
        variant.setPrice(variantPrice);
        variant.setProduct(new Product());
        return variant;
    }


    @Test
    public void testOtp(){

        TestObserver<OrderToPost> testObserver = new TestObserver<OrderToPost>();

        RestRepository.Companion.getINSTANCE()
                .postOrder(OrdersModel.Companion.getInstance().getKOrder().getOrderToPost(), "")
                .subscribe(testObserver);

        testObserver.assertTerminated();
        testObserver.assertError(AccessException.class);
    }


}
