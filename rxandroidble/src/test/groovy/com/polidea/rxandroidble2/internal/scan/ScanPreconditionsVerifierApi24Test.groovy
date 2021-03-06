package com.polidea.rxandroidble2.internal.scan

import com.polidea.rxandroidble2.exceptions.BleScanException
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class ScanPreconditionsVerifierApi24Test extends Specification {

    private TestScheduler testScheduler = new TestScheduler()
    private ScanPreconditionsVerifierApi18 mockScanPreconditionVerifierApi18 = Mock ScanPreconditionsVerifierApi18
    private ScanPreconditionsVerifierApi24 objectUnderTest = new ScanPreconditionsVerifierApi24(mockScanPreconditionVerifierApi18, testScheduler)

    def setup() {
        testScheduler.advanceTimeTo(1, TimeUnit.MINUTES)
    }

    def "should perform checks in proper order"() {

        given:
        Scheduler scheduler = Mock Scheduler
        objectUnderTest = new ScanPreconditionsVerifierApi24(mockScanPreconditionVerifierApi18, scheduler)
        scheduler.now(TimeUnit.MILLISECONDS) >> TimeUnit.SECONDS.toMillis(5)

        when:
        objectUnderTest.verify()

        then:
        1 * mockScanPreconditionVerifierApi18.verify()

        then:
        thrown BleScanException
    }

    def "should proxy exception thrown by ScanPreconditionsVerifierApi18"() {

        given:
        def testException = new BleScanException(BleScanException.UNKNOWN_ERROR_CODE, new Date())
        mockScanPreconditionVerifierApi18.verify() >> { throw testException }

        when:
        objectUnderTest.verify()

        then:
        thrown BleScanException
    }

    def "should throw BleScanException.UNDOCUMENTED_SCAN_THROTTLE if called 6th time during a 30 second window"() {

        given:
        objectUnderTest.verify()
        objectUnderTest.verify()
        objectUnderTest.verify()
        objectUnderTest.verify()
        objectUnderTest.verify()

        when:
        objectUnderTest.verify()

        then:
        BleScanException e = thrown BleScanException
        e.getReason() == BleScanException.UNDOCUMENTED_SCAN_THROTTLE
        e.getRetryDateSuggestion() == (new Date(testScheduler.now(TimeUnit.MILLISECONDS) + TimeUnit.SECONDS.toMillis(30)))
    }

    def "should not throw BleScanException.UNDOCUMENTED_SCAN_THROTTLE if called 6th time after a 30 second window"() {

        given:
        objectUnderTest.verify()
        objectUnderTest.verify()
        objectUnderTest.verify()
        objectUnderTest.verify()
        objectUnderTest.verify()
        testScheduler.advanceTimeBy(31, TimeUnit.SECONDS)

        when:
        objectUnderTest.verify()

        then:
        notThrown Throwable
    }
}