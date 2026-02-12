package no.nav.doknotifikasjon.consumer.altinn2;

import jakarta.xml.bind.JAXBElement;

import javax.xml.namespace.QName;

public class JAXBWrapper {
	private static final String NAMESPACE = "http://schemas.altinn.no/services/ServiceEngine/Notification/2009/10";

	public static JAXBElement<String> ns(String localpart, String value) {
		return new JAXBElement<>(new QName(NAMESPACE, localpart), String.class, value);
	}

	public static <T> JAXBElement<T> ns(String localpart, Class<T> clazz, T value) {
		return new JAXBElement<>(new QName(NAMESPACE, localpart), clazz, value);
	}

	public static JAXBElement<Boolean> ns(String localpart, Boolean value) {
		return new JAXBElement<>(new QName(NAMESPACE, localpart), Boolean.class, value);
	}

	private JAXBWrapper(){}
}
