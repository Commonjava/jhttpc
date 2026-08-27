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
package org.commonjava.util.jhttpc.it.root;

import org.commonjava.util.jhttpc.it.AbstractSSLTestsIT;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;

/**
 * Created by jdcasey on 10/30/15.
 */
public class RootSignedIT
        extends AbstractSSLTestsIT
{
    @Override
    protected String getContainerId()
    {
        return "rootsigned";
    }

    @Override
    protected SiteConfigBuilder getSiteConfigBuilder()
            throws Exception
    {
        return getNormalSiteConfigBuilder();
    }
}
