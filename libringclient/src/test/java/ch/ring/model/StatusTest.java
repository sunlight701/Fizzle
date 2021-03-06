/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package ch.seme.model;

import org.junit.Test;

import ch.seme.model.TextMessage;

import static org.junit.Assert.assertEquals;

public class StatusTest {

    @Test
    public void fromString_test() throws Exception {
        ch.seme.model.TextMessage.Status[] values = ch.seme.model.TextMessage.Status.values();
        for (ch.seme.model.TextMessage.Status s : values) {

            assertEquals(ch.seme.model.TextMessage.Status.fromString(s.name()), s);
        }
    }

    @Test
    public void fromString_invalid_test() throws Exception {
        ch.seme.model.TextMessage.Status status = ch.seme.model.TextMessage.Status.fromString("abc");

        assertEquals(ch.seme.model.TextMessage.Status.UNKNOWN, status);
    }

    @Test
    public void fromString_null_test() throws Exception {
        ch.seme.model.TextMessage.Status status = ch.seme.model.TextMessage.Status.fromString(null);

        assertEquals(ch.seme.model.TextMessage.Status.UNKNOWN, status);
    }

    @Test
    public void fromInt_test() throws Exception {
        for (int i = 0; i < 5; i++) {
            ch.seme.model.TextMessage.Status status = ch.seme.model.TextMessage.Status.fromInt(i);
            ch.seme.model.TextMessage.Status[] values = ch.seme.model.TextMessage.Status.values();

            assertEquals(status, values[i]);
        }
    }

    @Test
    public void fromInt_invalid_test() throws Exception {
        ch.seme.model.TextMessage.Status status = ch.seme.model.TextMessage.Status.fromInt(-1);

        assertEquals(ch.seme.model.TextMessage.Status.UNKNOWN, status);
    }

    @Test
    public void toString_test() throws Exception {
        ch.seme.model.TextMessage.Status[] values = ch.seme.model.TextMessage.Status.values();
        for (TextMessage.Status s : values) {
            assertEquals(s.toString(), s.name());
        }
    }
}