#
#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

gem "buildr", "~>1.3"
require "buildr"
require "buildr/antlr"

# Keep this structure to allow the build system to update version numbers.
VERSION_NUMBER = "0.1-SNAPSHOT"
NEXT_VERSION = "0.1"

ANTLR_RT            = "org.antlr:antlr-runtime:jar:3.1.1"
ASM                 = "asm:asm:jar:3.1"
COMMONS             = struct(
  :collections      =>"commons-collections:commons-collections:jar:3.1",
  :lang             =>"commons-lang:commons-lang:jar:2.1",
  :logging          =>"commons-logging:commons-logging:jar:1.1",
  :primitives       =>"commons-primitives:commons-primitives:jar:1.0"
)
DERBY               = "org.apache.derby:derby:jar:10.4.1.3"
GERONIMO            = struct(
  :kernel           =>"org.apache.geronimo.modules:geronimo-kernel:jar:2.0.1",
  :transaction      =>"org.apache.geronimo.components:geronimo-transaction:jar:2.0.1",
  :connector        =>"org.apache.geronimo.components:geronimo-connector:jar:2.0.1"
)
HSQLDB              = "hsqldb:hsqldb:jar:1.8.0.7"
JAVAX               = struct(
  :transaction      =>"org.apache.geronimo.specs:geronimo-jta_1.1_spec:jar:1.1",
  :resource         =>"org.apache.geronimo.specs:geronimo-j2ee-connector_1.5_spec:jar:1.0",
  :persistence      =>"javax.persistence:persistence-api:jar:1.0",
  :rest             =>"javax.ws.rs:jsr311-api:jar:1.0"
)
JERSEY              = group("jersey-server", "jersey-client", "jersey-core", :under=>"com.sun.jersey", :version=>"1.0.1")
JETTY               = group("jetty", "jetty-util", "servlet-api-2.5", :under=>"org.mortbay.jetty", :version=>"6.1.11")
LOG4J               = "log4j:log4j:jar:1.2.15"
ODE                 = group("ode-rest-bpel-api", "ode-rest-bpel-compiler", "ode-rest-bpel-dao", "ode-rest-dao-jpa", 
                            "ode-rest-runtimes", "ode-rest-engine", "ode-rest-il-common", "ode-rest-jacob", 
                            "ode-rest-scheduler-simple", "ode-rest-utils", :under=>"org.apache.ode", :version=>"0.1")
OPENJPA             = ["org.apache.openjpa:openjpa:jar:1.1.0",
                       "net.sourceforge.serp:serp:jar:1.13.1"]
SIMPEL              = "com.intalio.simpel:simpel:jar:0.1"
TRANQL              = ["tranql:tranql-connector:jar:1.1", COMMONS.primitives]
WSDL4J              = "wsdl4j:wsdl4j:jar:1.6.2"
XERCES              = "xerces:xercesImpl:jar:2.8.1"

repositories.remote << "http://repo1.maven.org/maven2"
repositories.remote << "http://download.java.net/maven/2"
repositories.remote << "http://www.intalio.org/public/maven2"

desc "Simplex process execution server, tightly tied to SimPEL."
define "simplex" do
  project.version = VERSION_NUMBER
  project.group = "com.intalio.simplex"

  compile.options.source = "1.5"
  compile.options.target = "1.5"
  manifest["Implementation-Vendor"] = "Intalio, Inc."
  meta_inf << file("NOTICE") << file("LICENSE")

  local_libs = file(_("lib/e4x-grammar-0.2.jar")), file(_("lib/rhino-1.7R2pre-patched.jar"))

  compile.with local_libs, SIMPEL, ODE, LOG4J, JAVAX.transaction, HSQLDB, JERSEY, 
    JAVAX.rest, JETTY, WSDL4J, GERONIMO.transaction, JAVAX.resource

  test.with COMMONS.lang, COMMONS.logging, LOG4J, ASM, DERBY, ODE,
    TRANQL, OPENJPA, GERONIMO.connector, JAVAX.persistence,
    XERCES, ANTLR_RT, local_libs, COMMONS.collections
  package :jar

  package(:zip, :id=>'simplex-public-html').tap do |p|
    p.include _("src/main/public_html/")
  end

  package(:zip, :id=>'intalio-simplex').path("intalio-#{id}-#{version}").tap do |zip|
    zip.include meta_inf + ["README", "src/main/samples/"].map { |f| path_to(f) }

    zip.path('lib').include artifacts(SIMPEL, ODE, LOG4J, JAVAX.transaction, JAVAX.resource,
      COMMONS.lang, COMMONS.logging, LOG4J, WSDL4J, ASM, JERSEY, DERBY, TRANQL, OPENJPA, 
      GERONIMO.transaction, GERONIMO.connector, JAVAX.persistence, JAVAX.rest, JETTY, 
      XERCES, ANTLR_RT, local_libs, COMMONS.collections, local_libs),
      package(:zip, :id=>'simplex-public-html')

    packages.each do |pkg|
      unless pkg.id =~ /intalio-simplex/
        zip.include(pkg.to_s, :path=>'lib')
      end
    end

    zip.path('bin').include _('src/main/bin/run'), _('src/main/bin/run.bat')
    zip.path('log').include _('src/main/etc/log4j.properties'), _('src/main/etc/logging.properties')
  end
end

