package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.EndPointResult;
import no.altinn.springsoap.client.gen.EndPointResultList;
import no.altinn.springsoap.client.gen.NotificationResult;
import no.altinn.springsoap.client.gen.ObjectFactory;
import no.altinn.springsoap.client.gen.SendNotificationResultList;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3Response;
import no.altinn.springsoap.client.gen.TransportType;
import org.springframework.core.io.InputStreamSource;
import org.springframework.ws.mime.Attachment;
import org.springframework.ws.mime.AttachmentException;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapBodyException;
import org.springframework.ws.soap.SoapEnvelope;
import org.springframework.ws.soap.SoapEnvelopeException;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderException;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.w3c.dom.Document;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class AltinResponseFactory {
	private static ObjectFactory objectfactory = new ObjectFactory();

	public static SendStandaloneNotificationBasicV3Response generateAltinnResponse(
			TransportType transportType,
			String address
	) {
		SendStandaloneNotificationBasicV3Response sendStandaloneNotificationBasicV3Response =
				objectfactory.createSendStandaloneNotificationBasicV3Response();

		SendNotificationResultList sendNotificationResultList = objectfactory.createSendNotificationResultList();

		NotificationResult notificationResult = objectfactory.createNotificationResult();

		EndPointResultList endPointResultList = objectfactory.createEndPointResultList();

		EndPointResult endPointResult = objectfactory.createEndPointResult();

		endPointResult.setTransportType(transportType);
		endPointResult.setReceiverAddress(objectfactory.createEndPointResultReceiverAddress(address));

		endPointResultList.getEndPointResult().add(endPointResult);

		notificationResult.setEndPoints(objectfactory.createEndPointResultList(endPointResultList));

		sendNotificationResultList.getNotificationResult().add(notificationResult);

		sendStandaloneNotificationBasicV3Response.setSendStandaloneNotificationBasicV3Result(
				objectfactory.createSendNotificationResultList(sendNotificationResultList)
		);
		return sendStandaloneNotificationBasicV3Response;
	}

	public static SendStandaloneNotificationBasicV3Response generateEmptyAltinnResponse() {
		SendStandaloneNotificationBasicV3Response sendStandaloneNotificationBasicV3Response =
				objectfactory.createSendStandaloneNotificationBasicV3Response();

		SendNotificationResultList sendNotificationResultList = objectfactory.createSendNotificationResultList();

		sendStandaloneNotificationBasicV3Response.setSendStandaloneNotificationBasicV3Result(
				objectfactory.createSendNotificationResultList(sendNotificationResultList)
		);
		return sendStandaloneNotificationBasicV3Response;
	}

	public static SoapFaultClientException generateSoapFaultClientException(String reason) {
		return new SoapFaultClientException(
				new SoapMessage() {
					@Override
					public SoapEnvelope getEnvelope() throws SoapEnvelopeException {
						return null;
					}

					@Override
					public String getSoapAction() {
						return null;
					}

					@Override
					public void setSoapAction(String soapAction) {

					}

					@Override
					public SoapBody getSoapBody() throws SoapBodyException {
						return null;
					}

					@Override
					public SoapHeader getSoapHeader() throws SoapHeaderException {
						return null;
					}

					@Override
					public SoapVersion getVersion() {
						return null;
					}

					@Override
					public Document getDocument() {
						return null;
					}

					@Override
					public void setDocument(Document document) {

					}

					@Override
					public boolean hasFault() {
						return true;
					}

					@Override
					public QName getFaultCode() {
						return null;
					}

					@Override
					public String getFaultReason() {
						return reason;
					}

					@Override
					public boolean isXopPackage() {
						return false;
					}

					@Override
					public boolean convertToXopPackage() {
						return false;
					}

					@Override
					public Attachment getAttachment(String contentId) throws AttachmentException {
						return null;
					}

					@Override
					public Iterator<Attachment> getAttachments() throws AttachmentException {
						return null;
					}

					@Override
					public Attachment addAttachment(String contentId, File file) throws AttachmentException {
						return null;
					}

					@Override
					public Attachment addAttachment(String contentId, InputStreamSource inputStreamSource, String contentType) {
						return null;
					}

					@Override
					public Attachment addAttachment(String contentId, DataHandler dataHandler) {
						return null;
					}

					@Override
					public Source getPayloadSource() {
						return null;
					}

					@Override
					public Result getPayloadResult() {
						return null;
					}

					@Override
					public void writeTo(OutputStream outputStream) throws IOException {

					}
				}
		);
	}

}
