package com.ecommerceproject.dubaimagazinesalvador.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.ValveBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProxyConfiavelTest {

    @ParameterizedTest
    @ValueSource(strings = {"preproduction", "production"})
    void somenteProxyLocalPodeInformarIpEProtocolo(String perfil) throws Exception {
        Properties properties = new Properties();
        try (var input = getClass().getResourceAsStream("/application-" + perfil + ".properties")) {
            properties.load(input);
        }
        assertEquals("native", properties.getProperty("server.forward-headers-strategy"));
        var valve = new RemoteIpValve();
        valve.setInternalProxies(properties.getProperty("server.tomcat.remoteip.internal-proxies"));
        valve.setRemoteIpHeader(properties.getProperty("server.tomcat.remoteip.remote-ip-header"));
        valve.setProtocolHeader(properties.getProperty("server.tomcat.remoteip.protocol-header"));
        valve.setPortHeader(properties.getProperty("server.tomcat.remoteip.port-header"));

        Estado local = encaminhar(valve, "127.0.0.1");
        assertTrue(local.seguro());
        assertEquals("192.0.2.80", local.ip());

        // Nem a rede privada é confiável por padrão: apenas o proxy loopback.
        for (String endereco : new String[] {"192.0.2.10", "10.0.0.10", "192.168.0.2"}) {
            Estado externo = encaminhar(valve, endereco);
            assertFalse(externo.seguro());
            assertEquals(endereco, externo.ip());
        }
    }

    private Estado encaminhar(RemoteIpValve valve, String endereco) throws Exception {
        var coyote = new org.apache.coyote.Request();
        coyote.scheme().setString("http");
        coyote.protocol().setString("HTTP/1.1");
        coyote.serverName().setString("localhost");
        coyote.getMimeHeaders().setValue("X-Forwarded-For").setString("192.0.2.80");
        coyote.getMimeHeaders().setValue("X-Forwarded-Proto").setString("https");
        coyote.getMimeHeaders().setValue("X-Forwarded-Port").setString("443");
        coyote.getMimeHeaders().setValue("Forwarded").setString("for=10.0.0.99;proto=http");
        var request = new Request(new Connector(), coyote);
        request.setRemoteAddr(endereco);
        request.setRemoteHost(endereco);
        request.setServerPort(8081);
        request.setLocalPort(8081);
        var resultado = new AtomicReference<Estado>();
        valve.setNext(new ValveBase() {
            @Override
            public void invoke(Request entrada, Response resposta) {
                resultado.set(new Estado(entrada.getRemoteAddr(), entrada.isSecure()));
            }
        });
        valve.invoke(request, null);
        return resultado.get();
    }

    private record Estado(String ip, boolean seguro) { }
}
