#!/bin/bash
# Split PEM chain into individual certificates and import into Java truststore
n=0
imported=0
awk '/-----BEGIN CERTIFICATE-----/{f="/tmp/cert-"NR".pem"} f{print > f} /-----END CERTIFICATE-----/{f=""}' /tmp/corporate-ca.crt

for cert in /tmp/cert-*.pem; do
  if [ -f "$cert" ]; then
    echo "Attempting to import certificate $n from $cert"
    if keytool -importcert -noprompt -trustcacerts -alias "corporate-ca-$n" \
      -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit \
      -file "$cert" 2>&1; then
      imported=$((imported+1))
    else
      echo "Warning: Could not import $cert (may not be a CA certificate)"
    fi
    n=$((n+1))
  fi
done
echo "Successfully imported $imported of $n certificates"
