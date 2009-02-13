/*
 * Simplex, lightweight SimPEL server
 * Copyright (C) 2008-2009  Intalio, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.intalio.simplex.embed;

import org.apache.log4j.Logger;

import javax.transaction.TransactionManager;

/**
 * @author Matthieu Riou <mriou@intalio.com>
 */
public class GeronimoTxFactory {
    private static final Logger __log = Logger.getLogger(GeronimoTxFactory.class);

    /* Public no-arg contructor is required */
    public GeronimoTxFactory() {
    }

    public TransactionManager getTransactionManager() {
        __log.debug("Using embedded Geronimo transaction manager");
        try {
            Object obj = new org.apache.geronimo.transaction.manager.GeronimoTransactionManager();
            return (TransactionManager) obj;
        } catch (Exception except) {
            throw new IllegalStateException("Unable to instantiate Geronimo Transaction Manager", except);
        }
    }
}
