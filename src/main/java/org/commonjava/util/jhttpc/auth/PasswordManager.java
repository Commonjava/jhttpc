/*
 * Copyright (C) 2015-2024 Red Hat, Inc. (https://github.com/Commonjava/jhttpc)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.jhttpc.auth;

import org.commonjava.util.jhttpc.model.SiteConfig;

public interface PasswordManager
{

    void bind( String password, SiteConfig config, PasswordType type );

    void bind( String password, String siteId, PasswordType type );

    void bind( String password, PasswordKey id );

    void unbind( SiteConfig config, PasswordType type );

    void unbind( String siteId, PasswordType type );

    void unbind( PasswordKey id );

    String lookup( PasswordKey id );

}
