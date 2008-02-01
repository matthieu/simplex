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

gem "buildr", "~>1.2.4"
require "buildr"
require "buildr/antlr"

# Keep this structure to allow the build system to update version numbers.
VERSION_NUMBER = "1.2-SNAPSHOT"
NEXT_VERSION = "1.2"

ANTLR   = "org.antlr:antlr:jar:3.0.1"
ODE     = ["org.apache.ode:ode-bpel-compiler:jar:1.2-SNAPSHOT",
            "org.apache.ode:ode-bpel-obj:jar:1.2-SNAPSHOT",
            "org.apache.ode:ode-utils:jar:1.2-SNAPSHOT"]
WSDL4J  = "wsdl4j:wsdl4j:jar:1.6.2"
LOG4J  = "log4j:log4j:jar:1.2.15"

repositories.remote << "http://repo1.maven.org/maven2"

desc "ODE SimPEL process execution language."
define "simpel" do
  project.version = VERSION_NUMBER
  project.group = "org.apache.ode"

  compile.options.source = "1.5"
  compile.options.target = "1.5"
  manifest["Implementation-Vendor"] = "Apache Software Foundation"
  meta_inf << file("NOTICE")

  pkg_name = "org.apache.ode.simpel.antlr"
  compile.from antlr(_("src/main/antlr"), {:in_package=>pkg_name, :token=>pkg_name})
  compile.with ODE, LOG4J, WSDL4J, file(_("lib/e4x-grammar-0.1.jar")), file(_("lib/antlr-20071220.jar"))
  package :jar
end
