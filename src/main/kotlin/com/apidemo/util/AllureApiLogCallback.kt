package com.apidemo.util

import com.apidemo.util.ApiHelper.Companion.EXPECTED_JSON
import com.apidemo.util.ApiHelper.Companion.REQUEST_JSON
import io.qameta.allure.Allure
import io.qameta.allure.model.Status
import io.qameta.allure.model.StepResult
import org.xpathqs.gwt.isThen
import org.xpathqs.log.abstracts.ILogCallback
import org.xpathqs.log.message.IMessage
import org.xpathqs.log.message.tag

class AllureApiLogCallback: ILogCallback {
    private var curUUID = ""
    val started = HashSet<String>()

    override fun onComplete(msg: IMessage, canLog: Boolean) {
        if(started.contains(msg.bodyMessage.uuid.toString()) ) {
            if(msg.isThen) {

            }
            started.remove(msg.bodyMessage.uuid.toString())
            Allure.getLifecycle().stopStep(msg.bodyMessage.uuid.toString())
        }
    }

    override fun onLog(msg: IMessage, canLog: Boolean) {
        if(canLog && msg.body.isNotEmpty()) {
            val text = msg.bodyMessage.toString()

            val status = if(msg.tag == "error") Status.FAILED else Status.PASSED

            if(msg.bodyMessage.uuid.toString() == curUUID) {
                if(msg.tag == REQUEST_JSON) {
                    Allure.addAttachment(
                        REQUEST_JSON,
                        text
                    )
                } else {
                    val result = StepResult()
                        .setName(text)
                        .setStatus(status)

                    if(msg.isThen) {
                        result.description = "expected"
                    }
                    started.add(curUUID);
                    Allure.getLifecycle().startStep(
                        curUUID,
                        result
                    )
                }
            } else {
                if(msg.tag == REQUEST_JSON) {
                    Allure.addAttachment(
                        REQUEST_JSON,
                        text
                    )
                } else if(msg.tag == EXPECTED_JSON) {
                    Allure.addAttachment(
                        EXPECTED_JSON,
                        text
                    )
                } else {
                    Allure.step(text, status)
                }
            }
        }
    }

    override fun onStart(msg: IMessage) {
        curUUID = msg.bodyMessage.uuid.toString()
    }
}