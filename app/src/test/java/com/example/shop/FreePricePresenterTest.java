package com.example.shop;

import com.example.shop.model.BDUtilsKt;
import com.example.shop.model.KOrder;
import com.example.shop.model.KOrderLine;
import com.example.shop.model.OrdersModel;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;


@RunWith(RobolectricTestRunner.class)
@Config(/*manifest = "src/main/AndroidManifest.xml",*/ sdk = 26, packageName = "")


public class FreePricePresenterTest {


    private FreePriceView stubView;


    @Before
    public void setUp()  {

        stubView = new FreePriceView() {
            @Override
            public void updateValues(String moneyValue) {

            }

            @Override
            public void setMaxExceededState(double maxValue, String formattedValue) {

            }

            @Override
            public void closeDialog() {

            }
        };
    }



    @Test
    public void testMarkupEnteredValue() throws Exception {

        OrdersModel.Companion.getInstance().resetCurrentOrder();
        KOrder order = OrdersModel.Companion.getInstance().getKOrder();
        Variant variant = getVariant(1, 1000, 1);
        order.add(variant, BigDecimal.ONE);
        order.add(getVariant(2, 1000, 1), BigDecimal.ONE);
        order.add(getVariant(3, 1000, 1), BigDecimal.ONE);

        int lineIndex = 0;
        KOrderLine lineByIndex = order.getLineByIndex(lineIndex);
        if (lineByIndex == null)
            throw new Exception();

        long enteredValueCopecks = 150000;


        FreePricePresenter presenter = new FreePricePresenter(new FreePriceViewEmpty(),lineIndex, PublishSubject.create());
        presenter.setView(stubView);
        presenter.setCopecksValue(enteredValueCopecks);

        //наценка в полтора раза
        presenter.onOkHandler();
        assertEquals(0, lineByIndex.getTotalPrice().compareTo(BDUtilsKt.getValueFromCopecks(enteredValueCopecks)));
    }

    private Variant getVariant(int id,long variantPrice, int quantity) {
        Variant variant = new Variant();
        variant.setId(id);
        variant.setQuantity(quantity);
        variant.setPrice(variantPrice);

        variant.setProduct(new Product());

        return variant;
    }


}
