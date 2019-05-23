package com.example.shop;

import com.example.shop.model.KOrder;
import com.example.shop.model.KOrderLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;



@RunWith(RobolectricTestRunner.class)
public class DialogAmountTest {

    private KOrder order;
    private KOrderLine orderLine1;
    private KOrderLine orderLine2;
    private List<KOrderLine> orderLines;

    @Before
    public void init() {
        BigDecimal quantity = new BigDecimal(5);
        order = new KOrder();
        orderLines = new ArrayList<>();
        Variant variant1 = new Variant(1);
        variant1.setQuantity(5);

        Variant variant2 = new Variant(2);
        variant2.setQuantity(2);
        orderLine1 = new KOrderLine(variant1,quantity);
        orderLine2 = new KOrderLine(variant2,quantity);
    }

    @Test
    public void checkAmountDeleteWithTwoLines() {
        orderLines.add(orderLine1);
        orderLines.add(orderLine2);
        order.setOrderLines(new ArrayList<>(orderLines));
        int pos = 0;
        RefundAvailableUseCase refundAvailableUseCase = new RefundAvailableUseCase(order,pos);

        assertTrue(!refundAvailableUseCase.isAvailable(6));
        assertTrue(refundAvailableUseCase.isAvailable(5));
        assertTrue(refundAvailableUseCase.isAvailable(4));
        assertTrue(refundAvailableUseCase.isAvailable(3));
        assertTrue(refundAvailableUseCase.isAvailable(0));
    }

    @Test
    public void checkAmountDeleteWithOneLine() {
        orderLines.add(orderLine1);
        order.setOrderLines(new ArrayList<>(orderLines));
        int pos = 0;
        RefundAvailableUseCase refundAvailableUseCase = new RefundAvailableUseCase(order,pos);

        assertTrue(!refundAvailableUseCase.isAvailable(6));
        assertTrue(refundAvailableUseCase.isAvailable(5));
        assertTrue(refundAvailableUseCase.isAvailable(4));
        assertTrue(refundAvailableUseCase.isAvailable(3));
        assertTrue(!refundAvailableUseCase.isAvailable(0));
    }
}
