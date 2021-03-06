/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.deltaspike.core.impl.util;

import org.apache.deltaspike.core.api.util.ClassUtils;

import javax.enterprise.inject.Typed;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the internal helper class for low level access to JNDI
 */
@Typed()
public class JndiUtils
{
    private static final Logger LOG = Logger.getLogger(JndiUtils.class.getName());

    private static InitialContext initialContext = null;

    static
    {
        try
        {
            initialContext = new InitialContext();
        }
        catch (Exception e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JndiUtils()
    {
        // prevent instantiation
    }

    /**
     * Resolves an instance for the given name.
     *
     * @param name       current name
     * @param targetType target type
     * @param <T>        type
     * @return the found instance, null otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String name, Class<? extends T> targetType)
    {
        try
        {
            Object result = initialContext.lookup(name);

            if (result != null)
            {
                if (targetType.isAssignableFrom(result.getClass()))
                {
                    // we have a value and the type fits
                    return (T) result;
                }
                else if (result instanceof String) //but the target type != String
                {
                    // lookedUp might be a class name
                    try
                    {
                        Class<?> classOfResult = ClassUtils.loadClassForName((String) result);
                        if (targetType.isAssignableFrom(classOfResult))
                        {
                            try
                            {
                                return (T) classOfResult.newInstance();
                            }
                            catch (Exception e)
                            {
                                // could not create instance
                                LOG.log(Level.SEVERE, "Class " + classOfResult + " from JNDI lookup for name "
                                        + name + " could not be instantiated", e);
                            }
                        }
                        else
                        {
                            // lookedUpClass does not extend/implement expectedClass
                            LOG.log(Level.SEVERE, "JNDI lookup for key " + name
                                    + " returned class " + classOfResult.getName()
                                    + " which does not implement/extend the expected class"
                                    + targetType.getName());
                        }
                    }
                    catch (ClassNotFoundException cnfe)
                    {
                        // could not find class
                        LOG.log(Level.SEVERE, "Could not find Class " + result
                                + " from JNDI lookup for name " + name, cnfe);
                    }
                }
                else
                {
                    // we have a value, but the value does not fit
                    LOG.log(Level.SEVERE, "JNDI lookup for key " + name + " should return a value of "
                            + targetType + ", but returned " + result);
                }
            }

            return null;
        }
        catch (NamingException e)
        {
            //X TODO custom exception type needed - see UnhandledException
            throw new IllegalStateException("Could not get " + name + " from JNDI", e);
        }
    }
}
