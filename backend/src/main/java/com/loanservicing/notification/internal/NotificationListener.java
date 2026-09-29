package com.loanservicing.notification.internal;

import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.loan.LoanDefaultedEvent;
import com.loanservicing.payment.PaymentPostedEvent;
import com.loanservicing.payment.PaymentReturnedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The notification module only LISTENS to events. Nobody calls it directly.
 *
 * @TransactionalEventListener: runs only after the payment transaction has committed
 *                              (no receipt for a payment that was rolled back).
 * @Async: runs on another thread, so a slow email never slows down the payment API.
 *
 * Microservice note: this class becomes notification-service. Replace the Spring events
 * with a Kafka consumer (@KafkaListener) on the same event records - the logic stays the same.
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final BorrowerApi borrowerApi;
    private final NotificationSender sender;

    @Async
    @TransactionalEventListener
    public void onPaymentPosted(PaymentPostedEvent e) {
        BorrowerDto b = borrowerApi.get(e.borrowerId());
        if (!b.sendPaymentReceipts()) {
            return;
        }
        String body = e.loanPaidOff()
                ? "Congratulations! Your payment of " + e.amount() + " paid off your loan."
                : "We received your payment of " + e.amount() + ". Your next payment is due on " + e.nextDueDate() + ".";
        sender.send(b.email(), "Payment received (#" + e.paymentId() + ")", "Hello " + b.fullName() + ",\n" + body);
    }

    @Async
    @TransactionalEventListener
    public void onPaymentReturned(PaymentReturnedEvent e) {
        BorrowerDto b = borrowerApi.get(e.borrowerId());
        sender.send(b.email(), "Your payment was returned",
                "Hello " + b.fullName() + ",\nYour payment of " + e.amount() + " was returned by your bank ("
                        + e.reason() + "). An NSF fee of " + e.nsfFee() + " has been added to your loan. "
                        + "Please make the payment again.");
    }

    @Async
    @TransactionalEventListener
    public void onLoanDefaulted(LoanDefaultedEvent e) {
        BorrowerDto b = borrowerApi.get(e.borrowerId());
        if (!b.sendLateNotices()) {
            return;
        }
        sender.send(b.email(), "Loan " + e.loanNumber() + " is past due",
                "Hello " + b.fullName() + ",\nYour loan is " + e.daysPastDue()
                        + " days past due. Please contact us to bring it current.");
    }
}
