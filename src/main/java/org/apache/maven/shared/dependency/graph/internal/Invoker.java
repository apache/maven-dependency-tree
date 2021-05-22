package org.apache.maven.shared.dependency.graph.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;

/**
 * Invokes method on objects using reflection.
 */
final class Invoker
{
    private Invoker()
    {
        // do not instantiate
    }

    static <T extends Exception> Object invoke( Object object, String method, ExceptionHandler<T> exceptionHandler )
        throws T
    {
        return invoke( object.getClass(), object, method, exceptionHandler );
    }

    static <T extends Exception> Object invoke( Class<?> objectClazz, Object object, String method,
                                                ExceptionHandler<T> exceptionHandler )
        throws T
    {
        try
        {
            return objectClazz.getMethod( method ).invoke( object );
        }
        catch ( ReflectiveOperationException e )
        {
            throw exceptionHandler.create( e.getMessage(), e );
        }
    }

    static Object invoke( Object object, String method, Class<?> clazz, Object arg )
        throws DependencyGraphBuilderException
    {
        try
        {
            final Class<?> objectClazz = object.getClass();
            return objectClazz.getMethod( method, clazz ).invoke( object, arg );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
    }
    
    static <T extends Exception> Object invoke( Class<?> objectClazz, String staticMethod,
                                                               Class<?> argClazz, Object arg,
                                                               ExceptionHandler<T> exceptionHandler )
        throws T
    {
        try
        {
            return objectClazz.getMethod( staticMethod, argClazz ).invoke( null, arg );
        }
        catch ( ReflectiveOperationException e )
        {
            throw exceptionHandler.create( e.getMessage(), e );
        }
    }

    static <T extends Exception> Object invoke( Class<?> objectClazz, String staticMethod, Class<?> argClazz1,
                                                Class<?> argClazz2, Object arg1, Object arg2,
                                                ExceptionHandler<T> exceptionHandler )
        throws T
    {
        try
        {
            return objectClazz.getMethod( staticMethod, argClazz1, argClazz2 ).invoke( null, arg1, arg2 );
        }
        catch ( ReflectiveOperationException e )
        {
            throw exceptionHandler.create( e.getMessage(), e );
        }
    }
}
