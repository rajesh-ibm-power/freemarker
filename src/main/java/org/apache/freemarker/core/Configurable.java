/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.BigDecimalArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.ConservativeArithmeticEngine;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapperBuilder;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat;
import org.apache.freemarker.core.outputformat.impl.RTFOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.AndMatcher;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.FirstMatchTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.MergingTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.NotMatcher;
import org.apache.freemarker.core.templateresolver.OrMatcher;
import org.apache.freemarker.core.templateresolver.PathGlobMatcher;
import org.apache.freemarker.core.templateresolver.PathRegexMatcher;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormatFM2;
import org.apache.freemarker.core.util.FTLUtil;
import org.apache.freemarker.core.util.GenericParseException;
import org.apache.freemarker.core.util.OptInTemplateClassResolver;
import org.apache.freemarker.core.util._ClassUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

/**
 * This is a common superclass of {@link org.apache.freemarker.core.Configuration},
 * {@link org.apache.freemarker.core.Template}, and {@link Environment} classes.
 * It provides settings that are common to each of them. FreeMarker
 * uses a three-level setting hierarchy - the return value of every setting
 * getter method on <code>Configurable</code> objects inherits its value from its parent 
 * <code>Configurable</code> object, unless explicitly overridden by a call to a 
 * corresponding setter method on the object itself. The parent of an 
 * <code>Environment</code> object is a <code>Template</code> object, the
 * parent of a <code>Template</code> object is a <code>Configuration</code>
 * object.
 */
public class Configurable {
    static final String C_TRUE_FALSE = "true,false";
    
    private static final String NULL = "null";
    private static final String DEFAULT = "default";
    private static final String JVM_DEFAULT = "JVM default";
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String LOCALE_KEY_SNAKE_CASE = "locale";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String LOCALE_KEY_CAMEL_CASE = "locale";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String LOCALE_KEY = LOCALE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String NUMBER_FORMAT_KEY_SNAKE_CASE = "number_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String NUMBER_FORMAT_KEY_CAMEL_CASE = "numberFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String NUMBER_FORMAT_KEY = NUMBER_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String CUSTOM_NUMBER_FORMATS_KEY_SNAKE_CASE = "custom_number_formats";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE = "customNumberFormats";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String CUSTOM_NUMBER_FORMATS_KEY = CUSTOM_NUMBER_FORMATS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_FORMAT_KEY_SNAKE_CASE = "time_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_FORMAT_KEY_CAMEL_CASE = "timeFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TIME_FORMAT_KEY = TIME_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String DATE_FORMAT_KEY_SNAKE_CASE = "date_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String DATE_FORMAT_KEY_CAMEL_CASE = "dateFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String DATE_FORMAT_KEY = DATE_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String CUSTOM_DATE_FORMATS_KEY_SNAKE_CASE = "custom_date_formats";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE = "customDateFormats";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String CUSTOM_DATE_FORMATS_KEY = CUSTOM_DATE_FORMATS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String DATETIME_FORMAT_KEY_SNAKE_CASE = "datetime_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String DATETIME_FORMAT_KEY_CAMEL_CASE = "datetimeFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String DATETIME_FORMAT_KEY = DATETIME_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_ZONE_KEY_SNAKE_CASE = "time_zone";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_ZONE_KEY_CAMEL_CASE = "timeZone";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TIME_ZONE_KEY = TIME_ZONE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE = "sql_date_and_time_time_zone";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE = "sqlDateAndTimeTimeZone";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY = SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE = "template_exception_handler";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY_CAMEL_CASE = "templateExceptionHandler";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY = TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String ARITHMETIC_ENGINE_KEY_SNAKE_CASE = "arithmetic_engine";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String ARITHMETIC_ENGINE_KEY_CAMEL_CASE = "arithmeticEngine";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String ARITHMETIC_ENGINE_KEY = ARITHMETIC_ENGINE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String OBJECT_WRAPPER_KEY_SNAKE_CASE = "object_wrapper";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String OBJECT_WRAPPER_KEY_CAMEL_CASE = "objectWrapper";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String OBJECT_WRAPPER_KEY = OBJECT_WRAPPER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String BOOLEAN_FORMAT_KEY_SNAKE_CASE = "boolean_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String BOOLEAN_FORMAT_KEY_CAMEL_CASE = "booleanFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String BOOLEAN_FORMAT_KEY = BOOLEAN_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String OUTPUT_ENCODING_KEY_SNAKE_CASE = "output_encoding";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String OUTPUT_ENCODING_KEY_CAMEL_CASE = "outputEncoding";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String OUTPUT_ENCODING_KEY = OUTPUT_ENCODING_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String URL_ESCAPING_CHARSET_KEY_SNAKE_CASE = "url_escaping_charset";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String URL_ESCAPING_CHARSET_KEY_CAMEL_CASE = "urlEscapingCharset";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String URL_ESCAPING_CHARSET_KEY = URL_ESCAPING_CHARSET_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String AUTO_FLUSH_KEY_SNAKE_CASE = "auto_flush";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String AUTO_FLUSH_KEY_CAMEL_CASE = "autoFlush";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.17 */
    public static final String AUTO_FLUSH_KEY = AUTO_FLUSH_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE = "new_builtin_class_resolver";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE = "newBuiltinClassResolver";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.17 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY = NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String SHOW_ERROR_TIPS_KEY_SNAKE_CASE = "show_error_tips";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String SHOW_ERROR_TIPS_KEY_CAMEL_CASE = "showErrorTips";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.21 */
    public static final String SHOW_ERROR_TIPS_KEY = SHOW_ERROR_TIPS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String API_BUILTIN_ENABLED_KEY_SNAKE_CASE = "api_builtin_enabled";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String API_BUILTIN_ENABLED_KEY_CAMEL_CASE = "apiBuiltinEnabled";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.22 */
    public static final String API_BUILTIN_ENABLED_KEY = API_BUILTIN_ENABLED_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE = "log_template_exceptions";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE = "logTemplateExceptions";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.22 */
    public static final String LOG_TEMPLATE_EXCEPTIONS_KEY = LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.25 */
    public static final String LAZY_IMPORTS_KEY_SNAKE_CASE = "lazy_imports";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.25 */
    public static final String LAZY_IMPORTS_KEY_CAMEL_CASE = "lazyImports";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String LAZY_IMPORTS_KEY = LAZY_IMPORTS_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.25 */
    public static final String LAZY_AUTO_IMPORTS_KEY_SNAKE_CASE = "lazy_auto_imports";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.25 */
    public static final String LAZY_AUTO_IMPORTS_KEY_CAMEL_CASE = "lazyAutoImports";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String LAZY_AUTO_IMPORTS_KEY = LAZY_AUTO_IMPORTS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.25 */
    public static final String AUTO_IMPORT_KEY_SNAKE_CASE = "auto_import";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.25 */
    public static final String AUTO_IMPORT_KEY_CAMEL_CASE = "autoImport";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String AUTO_IMPORT_KEY = AUTO_IMPORT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.25 */
    public static final String AUTO_INCLUDE_KEY_SNAKE_CASE = "auto_include";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.25 */
    public static final String AUTO_INCLUDE_KEY_CAMEL_CASE = "autoInclude";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String AUTO_INCLUDE_KEY = AUTO_INCLUDE_KEY_SNAKE_CASE;
    
    private static final String[] SETTING_NAMES_SNAKE_CASE = new String[] {
        // Must be sorted alphabetically!
        API_BUILTIN_ENABLED_KEY_SNAKE_CASE,
        ARITHMETIC_ENGINE_KEY_SNAKE_CASE,
        AUTO_FLUSH_KEY_SNAKE_CASE,
        AUTO_IMPORT_KEY_SNAKE_CASE,
        AUTO_INCLUDE_KEY_SNAKE_CASE,
        BOOLEAN_FORMAT_KEY_SNAKE_CASE,
        CUSTOM_DATE_FORMATS_KEY_SNAKE_CASE,
        CUSTOM_NUMBER_FORMATS_KEY_SNAKE_CASE,
        DATE_FORMAT_KEY_SNAKE_CASE,
        DATETIME_FORMAT_KEY_SNAKE_CASE,
        LAZY_AUTO_IMPORTS_KEY_SNAKE_CASE,
        LAZY_IMPORTS_KEY_SNAKE_CASE,
        LOCALE_KEY_SNAKE_CASE,
        LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE,
        NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE,
        NUMBER_FORMAT_KEY_SNAKE_CASE,
        OBJECT_WRAPPER_KEY_SNAKE_CASE,
        OUTPUT_ENCODING_KEY_SNAKE_CASE,
        SHOW_ERROR_TIPS_KEY_SNAKE_CASE,
        SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE,
        TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE,
        TIME_FORMAT_KEY_SNAKE_CASE,
        TIME_ZONE_KEY_SNAKE_CASE,
        URL_ESCAPING_CHARSET_KEY_SNAKE_CASE
    };
    
    private static final String[] SETTING_NAMES_CAMEL_CASE = new String[] {
        // Must be sorted alphabetically!
        API_BUILTIN_ENABLED_KEY_CAMEL_CASE,
        ARITHMETIC_ENGINE_KEY_CAMEL_CASE,
        AUTO_FLUSH_KEY_CAMEL_CASE,
        AUTO_IMPORT_KEY_CAMEL_CASE,
        AUTO_INCLUDE_KEY_CAMEL_CASE,
        BOOLEAN_FORMAT_KEY_CAMEL_CASE,
        CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE,
        CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE,
        DATE_FORMAT_KEY_CAMEL_CASE,
        DATETIME_FORMAT_KEY_CAMEL_CASE,
        LAZY_AUTO_IMPORTS_KEY_CAMEL_CASE,
        LAZY_IMPORTS_KEY_CAMEL_CASE,
        LOCALE_KEY_CAMEL_CASE,
        LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE,
        NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE,
        NUMBER_FORMAT_KEY_CAMEL_CASE,
        OBJECT_WRAPPER_KEY_CAMEL_CASE,
        OUTPUT_ENCODING_KEY_CAMEL_CASE,
        SHOW_ERROR_TIPS_KEY_CAMEL_CASE,
        SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE,
        TEMPLATE_EXCEPTION_HANDLER_KEY_CAMEL_CASE,
        TIME_FORMAT_KEY_CAMEL_CASE,
        TIME_ZONE_KEY_CAMEL_CASE,
        URL_ESCAPING_CHARSET_KEY_CAMEL_CASE
    };

    private Configurable parent;
    private HashMap<Object, Object> customAttributes;
    
    private Locale locale;
    private String numberFormat;
    private String timeFormat;
    private String dateFormat;
    private String dateTimeFormat;
    private TimeZone timeZone;
    private TimeZone sqlDataAndTimeTimeZone;
    private boolean sqlDataAndTimeTimeZoneSet;
    private String booleanFormat;
    private String trueStringValue;  // deduced from booleanFormat
    private String falseStringValue;  // deduced from booleanFormat
    private TemplateExceptionHandler templateExceptionHandler;
    private ArithmeticEngine arithmeticEngine;
    private ObjectWrapper objectWrapper;
    private String outputEncoding;
    private boolean outputEncodingSet;
    private String urlEscapingCharset;
    private boolean urlEscapingCharsetSet;
    private Boolean autoFlush;
    private TemplateClassResolver newBuiltinClassResolver;
    private Boolean showErrorTips;
    private Boolean apiBuiltinEnabled;
    private Boolean logTemplateExceptions;
    private Map<String, ? extends TemplateDateFormatFactory> customDateFormats;
    private Map<String, ? extends TemplateNumberFormatFactory> customNumberFormats;
    private LinkedHashMap<String, String> autoImports;
    private ArrayList<String> autoIncludes;
    private Boolean lazyImports;
    private Boolean lazyAutoImports;
    private boolean lazyAutoImportsSet;
    
    /**
     * Intended to be called from inside FreeMarker only.
     * Creates a top-level configurable, one that doesn't inherit from a parent, and thus stores the default values.
     * Called by the {@link Configuration} constructor.
     */
    protected Configurable(Version incompatibleImprovements) {
        _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        parent = null;
        locale = Locale.getDefault();
        timeZone = TimeZone.getDefault();
        sqlDataAndTimeTimeZone = null;
        numberFormat = "number";
        timeFormat = "";
        dateFormat = "";
        dateTimeFormat = "";
        templateExceptionHandler = Configuration.getDefaultTemplateExceptionHandler();
        arithmeticEngine = BigDecimalArithmeticEngine.INSTANCE;
        objectWrapper = Configuration.getDefaultObjectWrapper(incompatibleImprovements);
        autoFlush = Boolean.TRUE;
        newBuiltinClassResolver = TemplateClassResolver.UNRESTRICTED_RESOLVER;
        showErrorTips = Boolean.TRUE;
        apiBuiltinEnabled = Boolean.FALSE;
        logTemplateExceptions = Boolean.FALSE;
        // outputEncoding and urlEscapingCharset defaults to null,
        // which means "not specified"
        setBooleanFormat(C_TRUE_FALSE);
        
        customAttributes = new HashMap();
        
        customDateFormats = Collections.emptyMap();
        customNumberFormats = Collections.emptyMap();
        
        lazyImports = false;
        lazyAutoImportsSet = true;
        
        initAutoImportsMap();
        initAutoIncludesList();
    }

    /**
     * Creates a new instance. Normally you do not need to use this constructor,
     * as you don't use <code>Configurable</code> directly, but its subclasses.
     */
    protected Configurable(Configurable parent) {
        this.parent = parent;
        locale = null;
        numberFormat = null;
        templateExceptionHandler = null;
        customAttributes = new HashMap(0);
    }
    
    /**
     * Returns the parent {@link Configurable} object of this object. The parent stores the default setting values for
     * this {@link Configurable}. For example, the parent of a {@link org.apache.freemarker.core.Template} object is a
     * {@link Configuration} object, so values not specified on {@link Template}-level are get from the
     * {@link Configuration} object.
     * 
     * <p>
     * Note on the parent of {@link Environment}: If you set {@link Configuration#setIncompatibleImprovements(Version)
     * incompatible_improvements} to at least 2.3.22, it will be always the "main" {@link Template}, that is, the
     * template for whose processing the {@link Environment} was created. With lower {@code incompatible_improvements},
     * the current parent can temporary change <em>during template execution</em>, for example when your are inside an
     * {@code #include}-d template (among others). Thus, don't build on which {@link Template} the parent of
     * {@link Environment} is during template execution, unless you set {@code incompatible_improvements} to 2.3.22 or
     * higher.
     *
     * @return The parent {@link Configurable} object, or {@code null} if this is the root {@link Configurable} object
     *         (i.e, if it's the {@link Configuration} object).
     */
    public final Configurable getParent() {
        return parent;
    }
    
    /**
     * Reparenting support. This is used by Environment when it includes a
     * template - the included template becomes the parent configurable during
     * its evaluation.
     */
    void setParent(Configurable parent) {
        this.parent = parent;
    }
    
    /**
     * Sets the default locale used for number and date formatting (among others), also the locale used for searching
     * localized template variations when no locale was explicitly requested.
     * 
     * @see Configuration#getTemplate(String, Locale)
     */
    public void setLocale(Locale locale) {
        _NullArgumentException.check("locale", locale);
        this.locale = locale;
    }

    /**
     * Returns the assumed locale when searching for template files with no
     * explicit requested locale. Defaults to system locale.
     */
    public Locale getLocale() {
        return locale != null ? locale : parent.getLocale();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isLocaleSet() {
        return locale != null;
    }
    
    /**
     * Sets the time zone to use when formatting date/time values.
     * Defaults to the system time zone ({@link TimeZone#getDefault()}), regardless of the "locale" FreeMarker setting,
     * so in a server application you probably want to set it explicitly in the {@link Environment} to match the
     * preferred time zone of target audience (like the Web page visitor).
     * 
     * <p>If you or the templates set the time zone, you should probably also set
     * {@link #setSQLDateAndTimeTimeZone(TimeZone)}!
     * 
     * @see #setSQLDateAndTimeTimeZone(TimeZone)
     */
    public void setTimeZone(TimeZone timeZone) {
        _NullArgumentException.check("timeZone", timeZone);
        this.timeZone = timeZone;
    }

    /**
     * The getter pair of {@link #setTimeZone(TimeZone)}. 
     */
    public TimeZone getTimeZone() {
        return timeZone != null ? timeZone : parent.getTimeZone();
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isTimeZoneSet() {
        return timeZone != null;
    }
    
    /**
     * Sets the time zone used when dealing with {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. It defaults to {@code null} for backward compatibility, but in most
     * application this should be set to the JVM default time zone (server default time zone), because that's what
     * most JDBC drivers will use when constructing the {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. If this setting is {@code null}, FreeMarker will use the value of
     * ({@link #getTimeZone()}) for {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values,
     * which often gives bad results.
     * 
     * <p>This setting doesn't influence the formatting of other kind of values (like of
     * {@link java.sql.Timestamp java.sql.Timestamp} or plain {@link java.util.Date java.util.Date} values).
     * 
     * <p>To decide what value you need, a few things has to be understood:
     * <ul>
     *   <li>Date-only and time-only values in SQL-oriented databases are usually store calendar and clock field
     *   values directly (year, month, day, or hour, minute, seconds (with decimals)), as opposed to a set of points
     *   on the physical time line. Thus, unlike SQL timestamps, these values usually aren't meant to be shown
     *   differently depending on the time zone of the audience.
     *   
     *   <li>When a JDBC query has to return a date-only or time-only value, it has to convert it to a point on the
     *   physical time line, because that's what {@link java.util.Date} and its subclasses store (milliseconds since
     *   the epoch). Obviously, this is impossible to do. So JDBC just chooses a physical time which, when rendered
     *   <em>with the JVM default time zone</em>, will give the same field values as those stored
     *   in the database. (Actually, you can give JDBC a calendar, and so it can use other time zones too, but most
     *   application won't care using those overloads.) For example, assume that the system time zone is GMT+02:00.
     *   Then, 2014-07-12 in the database will be translated to physical time 2014-07-11 22:00:00 UTC, because that
     *   rendered in GMT+02:00 gives 2014-07-12 00:00:00. Similarly, 11:57:00 in the database will be translated to
     *   physical time 1970-01-01 09:57:00 UTC. Thus, the physical time stored in the returned value depends on the
     *   default system time zone of the JDBC client, not just on the content in the database. (This used to be the
     *   default behavior of ORM-s, like Hibernate, too.)
     *   
     *   <li>The value of the {@code time_zone} FreeMarker configuration setting sets the time zone used for the
     *   template output. For example, when a web page visitor has a preferred time zone, the web application framework
     *   may calls {@link Environment#setTimeZone(TimeZone)} with that time zone. Thus, the visitor will
     *   see {@link java.sql.Timestamp java.sql.Timestamp} and plain {@link java.util.Date java.util.Date} values as
     *   they look in his own time zone. While
     *   this is desirable for those types, as they meant to represent physical points on the time line, this is not
     *   necessarily desirable for date-only and time-only values. When {@code sql_date_and_time_time_zone} is
     *   {@code null}, {@code time_zone} is used for rendering all kind of date/time/dateTime values, including
     *   {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time}, and then if, for example,
     *   {@code time_zone} is GMT+00:00, the
     *   values from the earlier examples will be shown as 2014-07-11 (one day off) and 09:57:00 (2 hours off). While
     *   those are the time zone correct renderings, those values are probably meant to be shown "as is".
     *   
     *   <li>You may wonder why this setting isn't simply "SQL time zone", since the time zone related behavior of JDBC
     *   applies to {@link java.sql.Timestamp java.sql.Timestamp} too. FreeMarker assumes that you have set up your
     *   application so that time stamps coming from the database go through the necessary conversion to store the
     *   correct distance from the epoch (1970-01-01 00:00:00 UTC), as requested by {@link java.util.Date}. In that case
     *   the time stamp can be safely rendered in different time zones, and thus it needs no special treatment.
     * </ul>
     * 
     * @param tz Maybe {@code null}, in which case {@link java.sql.Date java.sql.Date} and
     *          {@link java.sql.Time java.sql.Time} values will be formatted in the time zone returned by
     *          {@link #getTimeZone()}.
     *          (Note that since {@code null} is an allowed value for this setting, it will not cause
     *          {@link #getSQLDateAndTimeTimeZone()} to fall back to the parent configuration.)
     * 
     * @see #setTimeZone(TimeZone)
     * 
     * @since 2.3.21
     */
    public void setSQLDateAndTimeTimeZone(TimeZone tz) {
        sqlDataAndTimeTimeZone = tz;
        sqlDataAndTimeTimeZoneSet = true;
    }
    
    /**
     * The getter pair of {@link #setSQLDateAndTimeTimeZone(TimeZone)}.
     * 
     * @return {@code null} if the value of {@link #getTimeZone()} should be used for formatting
     *     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values, otherwise the time zone
     *     that should be used to format the values of those two types.  
     * 
     * @since 2.3.21
     */
    public TimeZone getSQLDateAndTimeTimeZone() {
        return sqlDataAndTimeTimeZoneSet
                ? sqlDataAndTimeTimeZone
                : (parent != null ? parent.getSQLDateAndTimeTimeZone() : null);
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isSQLDateAndTimeTimeZoneSet() {
        return sqlDataAndTimeTimeZoneSet;
    }

    /**
     * Sets the default number format used to convert numbers to strings. Currently, this is one of these:
     * <ul>
     *   <li>{@code "number"}: The number format returned by {@link NumberFormat#getNumberInstance(Locale)}</li>
     *   <li>{@code "currency"}: The number format returned by {@link NumberFormat#getCurrencyInstance(Locale)}</li>
     *   <li>{@code "percent"}: The number format returned by {@link NumberFormat#getPercentInstance(Locale)}</li>
     *   <li>{@code "computer"}: The number format used by FTL's {@code c} built-in (like in {@code someNumber?c}).</li>
     *   <li>{@link java.text.DecimalFormat} pattern (like {@code "0.##"}). This syntax has a FreeMarker-specific
     *       extension, so that you can specify options like the rounding mode and the symbols used in this string. For
     *       example, {@code ",000;; roundingMode=halfUp groupingSeparator=_"} will format numbers like {@code ",000"}
     *       would, but with half-up rounding mode, and {@code _} as the group separator. See more about "extended Java
     *       decimal format" in the FreeMarker Manual.
     *       </li>
     *   <li>If the string starts with {@code @} character followed by a letter then it's interpreted as a custom number
     *       format, but only if either {@link Configuration#getIncompatibleImprovements()} is at least 2.3.24, or
     *       there's any custom formats defined (even if custom date/time/dateTime format). The format of a such string
     *       is <code>"@<i>name</i>"</code> or <code>"@<i>name</i> <i>parameters</i>"</code>, where
     *       <code><i>name</i></code> is the key in the {@link Map} set by {@link #setCustomNumberFormats(Map)}, and
     *       <code><i>parameters</i></code> is parsed by the custom {@link TemplateNumberFormat}.
     *   </li>
     * </ul>
     * 
     *   
     * <p>Defaults to <tt>"number"</tt>.
     */
    public void setNumberFormat(String numberFormat) {
        _NullArgumentException.check("numberFormat", numberFormat);
        this.numberFormat = numberFormat;
    }
    
    /**
     * Getter pair of {@link #setNumberFormat(String)}. 
     */
    public String getNumberFormat() {
        return numberFormat != null ? numberFormat : parent.getNumberFormat();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isNumberFormatSet() {
        return numberFormat != null;
    }
    
    /**
     * Getter pair of {@link #setCustomNumberFormats(Map)}; do not modify the returned {@link Map}! To be consistent
     * with other setting getters, if this setting was set directly on this {@link Configurable} object, this simply
     * returns that value, otherwise it returns the value from the parent {@link Configurable}. So beware, the returned
     * value doesn't reflect the {@link Map} key granularity fallback logic that FreeMarker actually uses for this
     * setting (for that, use {@link #getCustomNumberFormat(String)}). The returned value isn't a snapshot; it may or
     * may not shows the changes later made to this setting on this {@link Configurable} level (but usually it's well
     * defined if until what point settings are possibly modified).
     * 
     * <p>
     * The return value is never {@code null}; called on the {@link Configuration} (top) level, it defaults to an empty
     * {@link Map}.
     * 
     * @see #getCustomNumberFormatsWithoutFallback()
     * 
     * @since 2.3.24
     */
    public Map<String, ? extends TemplateNumberFormatFactory> getCustomNumberFormats() {
        return customNumberFormats == null ? parent.getCustomNumberFormats() : customNumberFormats;
    }

    /**
     * Like {@link #getCustomNumberFormats()}, but doesn't fall back to the parent {@link Configurable}, nor does it
     * provide a non-{@code null} default when called as the method of a {@link Configuration}.
     * 
     * @since 2.3.25
     */
    public Map<String, ? extends TemplateNumberFormatFactory> getCustomNumberFormatsWithoutFallback() {
        return customNumberFormats;
    }
    
    /**
     * Associates names with formatter factories, which then can be referred by the {@link #setNumberFormat(String)
     * number_format} setting with values starting with <code>@<i>name</i></code>. Beware, if you specify any custom
     * formats here, an initial {@code @} followed by a letter will have special meaning in number/date/time/datetime
     * format strings, even if {@link Configuration#getIncompatibleImprovements() incompatible_improvements} is less
     * than 2.3.24 (starting with {@link Configuration#getIncompatibleImprovements() incompatible_improvements} 2.3.24
     * {@code @} always has special meaning).
     * 
     * @param customNumberFormats
     *            Can't be {@code null}. The name must start with an UNICODE letter, and can only contain UNICODE
     *            letters and digits (not {@code _}).
     * 
     * @since 2.3.24
     */
    public void setCustomNumberFormats(Map<String, ? extends TemplateNumberFormatFactory> customNumberFormats) {
        _NullArgumentException.check("customNumberFormats", customNumberFormats);
        validateFormatNames(customNumberFormats.keySet());
        this.customNumberFormats = customNumberFormats;
    }
    
    private void validateFormatNames(Set<String> keySet) {
        for (String name : keySet) {
            if (name.length() == 0) {
                throw new IllegalArgumentException("Format names can't be 0 length");
            }
            char firstChar = name.charAt(0);
            if (firstChar == '@') {
                throw new IllegalArgumentException(
                        "Format names can't start with '@'. '@' is only used when referring to them from format "
                        + "strings. In: " + name);
            }
            if (!Character.isLetter(firstChar)) {
                throw new IllegalArgumentException("Format name must start with letter: " + name);
            }
            for (int i = 1; i < name.length(); i++) {
                // Note that we deliberately don't allow "_" here.
                if (!Character.isLetterOrDigit(name.charAt(i))) {
                    throw new IllegalArgumentException("Format name can only contain letters and digits: " + name);
                }
            }
        }
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isCustomNumberFormatsSet() {
        return customNumberFormats != null;
    }

    /**
     * Gets the custom name format registered for the name.
     * 
     * @since 2.3.24
     */
    public TemplateNumberFormatFactory getCustomNumberFormat(String name) {
        TemplateNumberFormatFactory r;
        if (customNumberFormats != null) {
            r = customNumberFormats.get(name);
            if (r != null) {
                return r;
            }
        }
        return parent != null ? parent.getCustomNumberFormat(name) : null;
    }
    
    /**
     * Tells if this configurable object or its parent defines any custom formats.
     * 
     * @since 2.3.24
     */
    public boolean hasCustomFormats() {
        return customNumberFormats != null && !customNumberFormats.isEmpty()
                || customDateFormats != null && !customDateFormats.isEmpty()
                || getParent() != null && getParent().hasCustomFormats(); 
    }
    
    /**
     * The string value for the boolean {@code true} and {@code false} values, intended for human audience (not for a
     * computer language), separated with comma. For example, {@code "yes,no"}. Note that white-space is significant,
     * so {@code "yes, no"} is WRONG (unless you want that leading space before "no").
     * 
     * <p>For backward compatibility the default is {@code "true,false"}, but using that value is denied for automatic
     * boolean-to-string conversion (like <code>${myBoolean}</code> will fail with it), only {@code myBool?string} will
     * allow it, which is deprecated since FreeMarker 2.3.20.
     * 
     * <p>Note that automatic boolean-to-string conversion only exists since FreeMarker 2.3.20. Earlier this setting
     * only influenced the result of {@code myBool?string}. 
     */
    public void setBooleanFormat(String booleanFormat) {
        _NullArgumentException.check("booleanFormat", booleanFormat);
        
        int commaIdx = booleanFormat.indexOf(',');
        if (commaIdx == -1) {
            throw new IllegalArgumentException(
                    "Setting value must be string that contains two comma-separated values for true and false, " +
                    "respectively.");
        }
        
        this.booleanFormat = booleanFormat; 
        
        if (booleanFormat.equals(C_TRUE_FALSE)) {
            // C_TRUE_FALSE is the default for BC, but it's not a good default for human audience formatting, so we
            // pretend that it wasn't set.
            trueStringValue = null; 
            falseStringValue = null;
        } else {
            trueStringValue = booleanFormat.substring(0, commaIdx); 
            falseStringValue = booleanFormat.substring(commaIdx + 1);
        }
    }
    
    /**
     * The getter pair of {@link #setBooleanFormat(String)}.
     */
    public String getBooleanFormat() {
        return booleanFormat != null ? booleanFormat : parent.getBooleanFormat(); 
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isBooleanFormatSet() {
        return booleanFormat != null;
    }
        
    String formatBoolean(boolean value, boolean fallbackToTrueFalse) throws TemplateException {
        if (value) {
            String s = getTrueStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return MiscUtil.C_TRUE;
                } else {
                    throw new _MiscTemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        } else {
            String s = getFalseStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return MiscUtil.C_FALSE;
                } else {
                    throw new _MiscTemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        }
    }

    private _ErrorDescriptionBuilder getNullBooleanFormatErrorDescription() {
        return new _ErrorDescriptionBuilder(
                "Can't convert boolean to string automatically, because the \"", BOOLEAN_FORMAT_KEY ,"\" setting was ",
                new _DelayedJQuote(getBooleanFormat()), 
                (getBooleanFormat().equals(C_TRUE_FALSE)
                    ? ", which is the legacy default computer-language format, and hence isn't accepted."
                    : ".")
                ).tips(
                     "If you just want \"true\"/\"false\" result as you are generting computer-language output, "
                     + "use \"?c\", like ${myBool?c}.",
                     "You can write myBool?string('yes', 'no') and like to specify boolean formatting in place.",
                     new Object[] {
                         "If you need the same two values on most places, the programmers should set the \"",
                         BOOLEAN_FORMAT_KEY ,"\" setting to something like \"yes,no\"." }
                 );
    }

    /**
     * Returns the string to which {@code true} is converted to for human audience, or {@code null} if automatic
     * coercion to string is not allowed. The default value is {@code null}.
     * 
     * <p>This value is deduced from the {@code "boolean_format"} setting.
     * Confusingly, for backward compatibility (at least until 2.4) that defaults to {@code "true,false"}, yet this
     * defaults to {@code null}. That's so because {@code "true,false"} is treated exceptionally, as that default is a
     * historical mistake in FreeMarker, since it targets computer language output, not human writing. Thus it's
     * ignored.
     * 
     * @since 2.3.20
     */
    String getTrueStringValue() {
        // The first step deliberately tests booleanFormat instead of trueStringValue! 
        return booleanFormat != null ? trueStringValue : (parent != null ? parent.getTrueStringValue() : null); 
    }

    /**
     * Same as {@link #getTrueStringValue()} but with {@code false}. 
     * @since 2.3.20
     */
    String getFalseStringValue() {
        // The first step deliberately tests booleanFormat instead of falseStringValue! 
        return booleanFormat != null ? falseStringValue : (parent != null ? parent.getFalseStringValue() : null); 
    }

    /**
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are time (no date part) values,
     * also the format that {@code someString?time} will use to parse strings.
     * 
     * <p>For the possible values see {@link #setDateTimeFormat(String)}.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@code "medium"}.
     */
    public void setTimeFormat(String timeFormat) {
        _NullArgumentException.check("timeFormat", timeFormat);
        this.timeFormat = timeFormat;
    }

    /**
     * The getter pair of {@link #setTimeFormat(String)}.
     */
    public String getTimeFormat() {
        return timeFormat != null ? timeFormat : parent.getTimeFormat();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isTimeFormatSet() {
        return timeFormat != null;
    }
    
    /**
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are date (no time part) values,
     * also the format that {@code someString?date} will use to parse strings.
     * 
     * <p>For the possible values see {@link #setDateTimeFormat(String)}.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@code "medium"}.
     */
    public void setDateFormat(String dateFormat) {
        _NullArgumentException.check("dateFormat", dateFormat);
        this.dateFormat = dateFormat;
    }

    /**
     * The getter pair of {@link #setDateFormat(String)}.
     */
    public String getDateFormat() {
        return dateFormat != null ? dateFormat : parent.getDateFormat();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isDateFormatSet() {
        return dateFormat != null;
    }
    
    /**
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are date-time (timestamp) values,
     * also the format that {@code someString?datetime} will use to parse strings.
     * 
     * <p>The possible setting values are (the quotation marks aren't part of the value itself):
     * 
     * <ul>
     *   <li><p>Patterns accepted by Java's {@link SimpleDateFormat}, for example {@code "dd.MM.yyyy HH:mm:ss"} (where
     *       {@code HH} means 24 hours format) or {@code "MM/dd/yyyy hh:mm:ss a"} (where {@code a} prints AM or PM, if
     *       the current language is English).
     *   
     *   <li><p>{@code "xs"} for XML Schema format, or {@code "iso"} for ISO 8601:2004 format.
     *       These formats allow various additional options, separated with space, like in
     *       {@code "iso m nz"} (or with {@code _}, like in {@code "iso_m_nz"}; this is useful in a case like
     *       {@code lastModified?string.iso_m_nz}). The options and their meanings are:
     *       
     *       <ul>
     *         <li><p>Accuracy options:<br>
     *             {@code ms} = Milliseconds, always shown with all 3 digits, even if it's all 0-s.
     *                     Example: {@code 13:45:05.800}<br>
     *             {@code s} = Seconds (fraction seconds are dropped even if non-0), like {@code 13:45:05}<br>
     *             {@code m} = Minutes, like {@code 13:45}. This isn't allowed for "xs".<br>
     *             {@code h} = Hours, like {@code 13}. This isn't allowed for "xs".<br>
     *             Neither = Up to millisecond accuracy, but trailing millisecond 0-s are removed, also the whole
     *                     milliseconds part if it would be 0 otherwise. Example: {@code 13:45:05.8}
     *                     
     *         <li><p>Time zone offset visibility options:<br>
     *             {@code fz} = "Force Zone", always show time zone offset (even for for
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values).
     *                     But, because ISO 8601 doesn't allow for dates (means date without time of the day) to
     *                     show the zone offset, this option will have no effect in the case of {@code "iso"} with
     *                     dates.<br>
     *             {@code nz} = "No Zone", never show time zone offset<br>
     *             Neither = always show time zone offset, except for {@link java.sql.Date java.sql.Date}
     *                     and {@link java.sql.Time java.sql.Time}, and for {@code "iso"} date values.
     *                     
     *         <li><p>Time zone options:<br>
     *             {@code u} = Use UTC instead of what the {@code time_zone} setting suggests. However,
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} aren't affected
     *                     by this (see {@link #setSQLDateAndTimeTimeZone(TimeZone)} to understand why)<br>
     *             {@code fu} = "Force UTC", that is, use UTC instead of what the {@code time_zone} or the
     *                     {@code sql_date_and_time_time_zone} setting suggests. This also effects
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values<br>
     *             Neither = Use the time zone suggested by the {@code time_zone} or the
     *                     {@code sql_date_and_time_time_zone} configuration setting ({@link #setTimeZone(TimeZone)} and
     *                     {@link #setSQLDateAndTimeTimeZone(TimeZone)}).
     *       </ul>
     *       
     *       <p>The options can be specified in any order.</p>
     *       
     *       <p>Options from the same category are mutually exclusive, like using {@code m} and {@code s}
     *       together is an error.
     *       
     *       <p>The accuracy and time zone offset visibility options don't influence parsing, only formatting.
     *       For example, even if you use "iso m nz", "2012-01-01T15:30:05.125+01" will be parsed successfully and with
     *       milliseconds accuracy.
     *       The time zone options (like "u") influence what time zone is chosen only when parsing a string that doesn't
     *       contain time zone offset.
     *       
     *       <p>Parsing with {@code "iso"} understands both extend format and basic format, like
     *       {@code 20141225T235018}. It doesn't, however, support the parsing of all kind of ISO 8601 strings: if
     *       there's a date part, it must use year, month and day of the month values (not week of the year), and the
     *       day can't be omitted.
     *       
     *       <p>The output of {@code "iso"} is deliberately so that it's also a good representation of the value with
     *       XML Schema format, except for 0 and negative years, where it's impossible. Also note that the time zone
     *       offset is omitted for date values in the {@code "iso"} format, while it's preserved for the {@code "xs"}
     *       format.
     *       
     *   <li><p>{@code "short"}, {@code "medium"}, {@code "long"}, or {@code "full"}, which that has locale-dependent
     *       meaning defined by the Java platform (see in the documentation of {@link java.text.DateFormat}).
     *       For date-time values, you can specify the length of the date and time part independently, be separating
     *       them with {@code _}, like {@code "short_medium"}. ({@code "medium"} means
     *       {@code "medium_medium"} for date-time values.)
     *       
     *   <li><p>Anything that starts with {@code "@"} followed by a letter is interpreted as a custom
     *       date/time/dateTime format, but only if either {@link Configuration#getIncompatibleImprovements()}
     *       is at least 2.3.24, or there's any custom formats defined (even if custom number format). The format of
     *       such string is <code>"@<i>name</i>"</code> or <code>"@<i>name</i> <i>parameters</i>"</code>, where
     *       <code><i>name</i></code> is the key in the {@link Map} set by {@link #setCustomDateFormats(Map)}, and
     *       <code><i>parameters</i></code> is parsed by the custom number format.
     *       
     * </ul> 
     * 
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@code "medium_medium"}.
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        _NullArgumentException.check("dateTimeFormat", dateTimeFormat);
        this.dateTimeFormat = dateTimeFormat;
    }

    /**
     * The getter pair of {@link #setDateTimeFormat(String)}.
     */
    public String getDateTimeFormat() {
        return dateTimeFormat != null ? dateTimeFormat : parent.getDateTimeFormat();
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isDateTimeFormatSet() {
        return dateTimeFormat != null;
    }
    
    /**
     * Getter pair of {@link #setCustomDateFormats(Map)}; do not modify the returned {@link Map}! To be consistent with
     * other setting getters, if this setting was set directly on this {@link Configurable} object, this simply returns
     * that value, otherwise it returns the value from the parent {@link Configurable}. So beware, the returned value
     * doesn't reflect the {@link Map} key granularity fallback logic that FreeMarker actually uses for this setting
     * (for that, use {@link #getCustomDateFormat(String)}). The returned value isn't a snapshot; it may or may not
     * shows the changes later made to this setting on this {@link Configurable} level (but usually it's well defined if
     * until what point settings are possibly modified).
     * 
     * <p>
     * The return value is never {@code null}; called on the {@link Configuration} (top) level, it defaults to an empty
     * {@link Map}.
     * 
     * @see #getCustomDateFormatsWithoutFallback()
     * 
     * @since 2.3.24
     */
    public Map<String, ? extends TemplateDateFormatFactory> getCustomDateFormats() {
        return customDateFormats == null ? parent.getCustomDateFormats() : customDateFormats;
    }

    /**
     * Like {@link #getCustomDateFormats()}, but doesn't fall back to the parent {@link Configurable}, nor does it
     * provide a non-{@code null} default when called as the method of a {@link Configuration}.
     * 
     * @since 2.3.25
     */
    public Map<String, ? extends TemplateDateFormatFactory> getCustomDateFormatsWithoutFallback() {
        return customDateFormats;
    }
    
    /**
     * Associates names with formatter factories, which then can be referred by the {@link #setDateTimeFormat(String)
     * date_format}, {@link #setDateTimeFormat(String) time_format}, and {@link #setDateTimeFormat(String)
     * datetime_format} settings with values starting with <code>@<i>name</i></code>. Beware, if you specify any custom
     * formats here, an initial {@code @} followed by a letter will have special meaning in number/date/time/datetime
     * format strings, even if {@link Configuration#getIncompatibleImprovements() incompatible_improvements} is less
     * than 2.3.24 (starting with {@link Configuration#getIncompatibleImprovements() incompatible_improvements} 2.3.24
     * {@code @} always has special meaning).
     *
     * @param customDateFormats
     *            Can't be {@code null}. The name must start with an UNICODE letter, and can only contain UNICODE
     *            letters and digits.
     * 
     * @since 2.3.24
     */
    public void setCustomDateFormats(Map<String, ? extends TemplateDateFormatFactory> customDateFormats) {
        _NullArgumentException.check("customDateFormats", customDateFormats);
        validateFormatNames(customDateFormats.keySet());
        this.customDateFormats = customDateFormats;
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     * 
     * @since 2.3.24
     */
    public boolean isCustomDateFormatsSet() {
        return customDateFormats != null;
    }

    /**
     * Gets the custom name format registered for the name.
     * 
     * @since 2.3.24
     */
    public TemplateDateFormatFactory getCustomDateFormat(String name) {
        TemplateDateFormatFactory r;
        if (customDateFormats != null) {
            r = customDateFormats.get(name);
            if (r != null) {
                return r;
            }
        }
        return parent != null ? parent.getCustomDateFormat(name) : null;
    }
    
    /**
     * Sets the exception handler used to handle exceptions occurring inside templates.
     * The default is {@link TemplateExceptionHandler#DEBUG_HANDLER}. The recommended values are:
     * 
     * <ul>
     *   <li>In production systems: {@link TemplateExceptionHandler#RETHROW_HANDLER}
     *   <li>During development of HTML templates: {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}
     *   <li>During development of non-HTML templates: {@link TemplateExceptionHandler#DEBUG_HANDLER}
     * </ul>
     * 
     * <p>All of these will let the exception propagate further, so that you can catch it around
     * {@link Template#process(Object, Writer)} for example. The difference is in what they print on the output before
     * they do that.
     * 
     * <p>Note that the {@link TemplateExceptionHandler} is not meant to be used for generating HTTP error pages.
     * Neither is it meant to be used to roll back the printed output. These should be solved outside template
     * processing when the exception raises from {@link Template#process(Object, Writer) Template.process}.
     * {@link TemplateExceptionHandler} meant to be used if you want to include special content <em>in</em> the template
     * output, or if you want to suppress certain exceptions. 
     */
    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        _NullArgumentException.check("templateExceptionHandler", templateExceptionHandler);
        this.templateExceptionHandler = templateExceptionHandler;
    }

    /**
     * The getter pair of {@link #setTemplateExceptionHandler(TemplateExceptionHandler)}.
     */
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return templateExceptionHandler != null
                ? templateExceptionHandler : parent.getTemplateExceptionHandler();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isTemplateExceptionHandlerSet() {
        return templateExceptionHandler != null;
    }

    /**
     * Sets the arithmetic engine used to perform arithmetic operations.
     * The default is {@link BigDecimalArithmeticEngine#INSTANCE}.
     */
    public void setArithmeticEngine(ArithmeticEngine arithmeticEngine) {
        _NullArgumentException.check("arithmeticEngine", arithmeticEngine);
        this.arithmeticEngine = arithmeticEngine;
    }

    /**
     * The getter pair of {@link #setArithmeticEngine(ArithmeticEngine)}.
     */
    public ArithmeticEngine getArithmeticEngine() {
        return arithmeticEngine != null
                ? arithmeticEngine : parent.getArithmeticEngine();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isArithmeticEngineSet() {
        return arithmeticEngine != null;
    }

    /**
     * Sets the object wrapper used to wrap objects to {@link TemplateModel}-s.
     * The default is {@link DefaultObjectWrapperBuilder#build()}.
     */
    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        _NullArgumentException.check("objectWrapper", objectWrapper);
        this.objectWrapper = objectWrapper;
    }

    /**
     * The getter pair of {@link #setObjectWrapper(ObjectWrapper)}.
     */
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper != null
                ? objectWrapper : parent.getObjectWrapper();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isObjectWrapperSet() {
        return objectWrapper != null;
    }
    
    /**
     * Informs FreeMarker about the charset used for the output. As FreeMarker outputs character stream (not
     * byte stream), it's not aware of the output charset unless the software that encloses it tells it
     * with this setting. Some templates may use FreeMarker features that require this information.
     * Setting this to {@code null} means that the output encoding is not known.
     * 
     * <p>Defaults to {@code null} (unknown).
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
        outputEncodingSet = true;
    }
    
    public String getOutputEncoding() {
        return outputEncodingSet
                ? outputEncoding
                : (parent != null ? parent.getOutputEncoding() : null);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isOutputEncodingSet() {
        return outputEncodingSet;
    }
    
    /**
     * Sets the URL escaping charset. If not set ({@code null}), the output encoding
     * ({@link #setOutputEncoding(String)}) will be used for URL escaping.
     * 
     * Defaults to {@code null}.
     */
    public void setURLEscapingCharset(String urlEscapingCharset) {
        this.urlEscapingCharset = urlEscapingCharset;
        urlEscapingCharsetSet = true;
    }
    
    public String getURLEscapingCharset() {
        return urlEscapingCharsetSet
                ? urlEscapingCharset
                : (parent != null ? parent.getURLEscapingCharset() : null);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isURLEscapingCharsetSet() {
        return urlEscapingCharsetSet;
    }

    /**
     * Sets the {@link TemplateClassResolver} that is used when the
     * <code>new</code> built-in is called in a template. That is, when
     * a template contains the <code>"com.example.SomeClassName"?new</code>
     * expression, this object will be called to resolve the
     * <code>"com.example.SomeClassName"</code> string to a class. The default
     * value is {@link TemplateClassResolver#UNRESTRICTED_RESOLVER}. If you allow
     * users to upload templates, it's important to use a custom restrictive
     * {@link TemplateClassResolver} or {@link TemplateClassResolver#ALLOWS_NOTHING_RESOLVER}.
     * 
     * @since 2.3.17
     */
    public void setNewBuiltinClassResolver(TemplateClassResolver newBuiltinClassResolver) {
        _NullArgumentException.check("newBuiltinClassResolver", newBuiltinClassResolver);
        this.newBuiltinClassResolver = newBuiltinClassResolver;
    }

    /**
     * Retrieves the {@link TemplateClassResolver} used
     * to resolve classes when "SomeClassName"?new is called in a template.
     * 
     * @see #setNewBuiltinClassResolver(TemplateClassResolver)
     * 
     * @since 2.3.17
     */
    public TemplateClassResolver getNewBuiltinClassResolver() {
        return newBuiltinClassResolver != null
                ? newBuiltinClassResolver : parent.getNewBuiltinClassResolver();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isNewBuiltinClassResolverSet() {
        return newBuiltinClassResolver != null;
    }
    
    /**
     * Sets whether the output {@link Writer} is automatically flushed at
     * the end of {@link Template#process(Object, Writer)} (and its
     * overloads). The default is {@code true}.
     * 
     * <p>Using {@code false} is needed for example when a Web page is composed
     * from several boxes (like portlets, GUI panels, etc.) that aren't inserted
     * with <tt>#include</tt> (or with similar directives) into a master
     * FreeMarker template, rather they are all processed with a separate
     * {@link Template#process(Object, Writer)} call. In a such scenario the
     * automatic flushes would commit the HTTP response after each box, hence
     * interfering with full-page buffering, and also possibly decreasing
     * performance with too frequent and too early response buffer flushes.
     * 
     * @since 2.3.17
     */
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = Boolean.valueOf(autoFlush);
    }
    
    /**
     * See {@link #setAutoFlush(boolean)}
     * 
     * @since 2.3.17
     */
    public boolean getAutoFlush() {
        return autoFlush != null 
            ? autoFlush.booleanValue()
            : (parent != null ? parent.getAutoFlush() : true);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isAutoFlushSet() {
        return autoFlush != null;
    }
    
    /**
     * Sets if tips should be shown in error messages of errors arising during template processing.
     * The default is {@code true}. 
     * 
     * @since 2.3.21
     */
    public void setShowErrorTips(boolean showTips) {
        showErrorTips = Boolean.valueOf(showTips);
    }
    
    /**
     * See {@link #setShowErrorTips(boolean)}
     * 
     * @since 2.3.21
     */
    public boolean getShowErrorTips() {
        return showErrorTips != null 
            ? showErrorTips.booleanValue()
            : (parent != null ? parent.getShowErrorTips() : true);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isShowErrorTipsSet() {
        return showErrorTips != null;
    }
    
    /**
     * Specifies if {@code ?api} can be used in templates. Defaults to {@code false} so that updating FreeMarker won't
     * decrease the security of existing applications.
     * 
     * @since 2.3.22
     */
    public void setAPIBuiltinEnabled(boolean value) {
        apiBuiltinEnabled = Boolean.valueOf(value);
    }

    /**
     * See {@link #setAPIBuiltinEnabled(boolean)}
     * 
     * @since 2.3.22
     */
    public boolean isAPIBuiltinEnabled() {
        return apiBuiltinEnabled != null 
                ? apiBuiltinEnabled.booleanValue()
                : (parent != null ? parent.isAPIBuiltinEnabled() : false);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isAPIBuiltinEnabledSet() {
        return apiBuiltinEnabled != null;
    }
    
    /**
     * Specifies if {@link TemplateException}-s thrown by template processing are logged by FreeMarker or not. The
     * default is {@code true} for backward compatibility, but that results in logging the exception twice in properly
     * written applications, because there the {@link TemplateException} thrown by the public FreeMarker API is also
     * logged by the caller (even if only as the cause exception of a higher level exception). Hence, in modern
     * applications it should be set to {@code false}. Note that this setting has no effect on the logging of exceptions
     * caught by {@code #attempt}; those are always logged, no mater what (because those exceptions won't bubble up
     * until the API caller).
     * 
     * @since 2.3.22
     */
    public void setLogTemplateExceptions(boolean value) {
        logTemplateExceptions = Boolean.valueOf(value);
    }

    /**
     * See {@link #setLogTemplateExceptions(boolean)}
     * 
     * @since 2.3.22
     */
    public boolean getLogTemplateExceptions() {
        return logTemplateExceptions != null 
                ? logTemplateExceptions.booleanValue()
                : (parent != null ? parent.getLogTemplateExceptions() : true);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.24
     */
    public boolean isLogTemplateExceptionsSet() {
        return logTemplateExceptions != null;
    }
    
    /**
     * The getter pair of {@link #setLazyImports(boolean)}.
     * 
     * @since 2.3.25
     */
    public boolean getLazyImports() {
        return lazyImports != null ? lazyImports.booleanValue() : parent.getLazyImports();
    }
    
    /**
     * Specifies if {@code <#import ...>} (and {@link Environment#importLib(String, String)}) should delay the loading
     * and processing of the imported templates until the content of the imported namespace is actually accessed. This
     * makes the overhead of <em>unused</em> imports negligible. A drawback is that importing a missing or otherwise
     * broken template will be successful, and the problem will remain hidden until (and if) the namespace content is
     * actually used. Also, you lose the strict control over when the namespace initializing code in the imported
     * template will be executed, though it shouldn't mater for well written imported templates anyway. Note that the
     * namespace initializing code will run with the same {@linkplain Configurable#getLocale() locale} as it was at the
     * point of the {@code <#import ...>} call (other settings won't be handled specially like that).
     * 
     * <p>
     * The default is {@code false} (and thus imports are eager) for backward compatibility, which can cause
     * perceivable overhead if you have many imports and only a few of them is used.
     * 
     * <p>
     * This setting also affects {@linkplain #setAutoImports(Map) auto-imports}, unless you have set a non-{@code null}
     * value with {@link #setLazyAutoImports(Boolean)}.
     * 
     * @see #setLazyAutoImports(Boolean)
     * 
     * @since 2.3.25
     */
    public void setLazyImports(boolean lazyImports) {
        this.lazyImports = Boolean.valueOf(lazyImports);
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.25
     */
    public boolean isLazyImportsSet() {
        return lazyImports != null;
    }
    
    /**
     * The getter pair of {@link #setLazyAutoImports(Boolean)}.
     * 
     * @since 2.3.25
     */
    public Boolean getLazyAutoImports() {
        return lazyAutoImportsSet ? lazyAutoImports : parent.getLazyAutoImports();
    }

    /**
     * Specifies if {@linkplain #setAutoImports(Map) auto-imports} will be
     * {@link #setLazyImports(boolean) lazy imports}. This is useful to make the overhead of <em>unused</em>
     * auto-imports negligible. If this is set to {@code null}, {@link #getLazyImports()} specifies the behavior of
     * auto-imports too. The default value is {@code null}.
     * 
     * @since 2.3.25
     */
    public void setLazyAutoImports(Boolean lazyAutoImports) {
        this.lazyAutoImports = lazyAutoImports;
        lazyAutoImportsSet = true;
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *  
     * @since 2.3.25
     */
    public boolean isLazyAutoImportsSet() {
        return lazyAutoImportsSet;
    }
    
    /**
     * Adds an invisible <code>#import <i>templateName</i> as <i>namespaceVarName</i></code> at the beginning of the
     * main template (that's the top-level template that wasn't included/imported from another template). While it only
     * affects the main template directly, as the imports will create a global variable there, the imports will be
     * visible from the further imported templates too (note that {@link Configuration#getIncompatibleImprovements()}
     * set to 2.3.24 fixes a rarely surfacing bug with that).
     * 
     * <p>
     * It's recommended to set the {@code auto_impots_lazy} setting ({@link Configuration#setLazyAutoImports(Boolean)})
     * to {@code true} when using this, so that auto-imports that are unused in a template won't degrade performance by
     * unnecessary loading and initializing the imported library.
     * 
     * <p>
     * If the imports aren't lazy, the order of the imports will be the same as the order in which they were added with
     * this method. (Calling this method with an already added {@code namespaceVarName} will move that to the end
     * of the auto-import order.)
     * 
     * <p>
     * The auto-import is added directly to the {@link Configurable} on which this method is called (not to the parents
     * or children), but when the main template is processed, the auto-imports are collected from all the
     * {@link Configurable} levels, in parent-to-child order: {@link Configuration}, {@link Template} (the main
     * template), {@link Environment}. If the same {@code namespaceVarName} occurs on multiple levels, the one on the
     * child level is used, and the clashing import from the parent level is skipped.
     * 
     * <p>If there are also auto-includes (see {@link #addAutoInclude(String)}), those will be executed after
     * the auto-imports.
     * 
     * @see #setAutoImports(Map)
     */
    public void addAutoImport(String namespaceVarName, String templateName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (autoImports == null) {
                initAutoImportsMap();
            } else {
                // This was a List earlier, so re-inserted items must go to the end, hence we remove() before put().
                autoImports.remove(namespaceVarName);
            }
            autoImports.put(namespaceVarName, templateName);
        }
    }

    private void initAutoImportsMap() {
        autoImports = new LinkedHashMap<>(4);
    }
    
    /**
     * Removes an auto-import from this {@link Configurable} level (not from the parents or children);
     * see {@link #addAutoImport(String, String)}. Does nothing if the auto-import doesn't exist.
     */
    public void removeAutoImport(String namespaceVarName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (autoImports != null) {
                autoImports.remove(namespaceVarName);
            }
        }
    }
    
    /**
     * Removes all auto-imports, then calls {@link #addAutoImport(String, String)} for each {@link Map}-entry (the entry
     * key is the {@code namespaceVarName}). The order of the auto-imports will be the same as {@link Map#keySet()}
     * returns the keys (but the order of imports doesn't mater for properly designed libraries anyway).
     * 
     * @param map
     *            Maps the namespace variable names to the template names; not {@code null}
     */
    public void setAutoImports(Map map) {
        _NullArgumentException.check("map", map);
        
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (autoImports != null) {
                autoImports.clear();
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException(
                            "Key in Map wasn't a String, but a(n) " + key.getClass().getName() + ".");
                }
                
                Object value = entry.getValue();
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException(
                            "Value in Map wasn't a String, but a(n) " + key.getClass().getName() + ".");
                }
                
                addAutoImport((String) key, (String) value);
            }
        }
    }
    
    /**
     * Getter pair of {@link #setAutoImports(Map)}; do not modify the returned {@link Map}! To be consistent with other
     * setting getters, if this setting was set directly on this {@link Configurable} object, this simply returns that
     * value, otherwise it returns the value from the parent {@link Configurable}. So beware, the returned value doesn't
     * reflect the {@link Map} key granularity fallback logic that FreeMarker actually uses for this setting. The
     * returned value is not the same {@link Map} object that was set with {@link #setAutoImports(Map)}, only its
     * content is the same. The returned value isn't a snapshot; it may or may not shows the changes later made to this
     * setting on this {@link Configurable} level (but usually it's well defined if until what point settings are
     * possibly modified).
     * 
     * <p>
     * The return value is never {@code null}; called on the {@link Configuration} (top) level, it defaults to an empty
     * {@link Map}.
     * 
     * @see #getAutoImportsWithoutFallback()
     * 
     * @since 2.3.25
     */
    public Map<String, String> getAutoImports() {
        return autoImports != null ? autoImports : parent.getAutoImports();
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     * 
     * @since 2.3.25
     */
    public boolean isAutoImportsSet() {
        return autoImports != null;
    }

    /**
     * Like {@link #getAutoImports()}, but doesn't fall back to the parent {@link Configurable} (and so it can be
     * {@code null}).
     *  
     * @since 2.3.25
     */
    public Map<String, String> getAutoImportsWithoutFallback() {
        return autoImports;
    }
    
    /**
     * Adds an invisible <code>#include <i>templateName</i></code> at the beginning of the main template (that's the
     * top-level template that wasn't included/imported from another template).
     * 
     * <p>
     * The order of the inclusions will be the same as the order in which they were added with this method.
     * 
     * <p>
     * The auto-include is added directly to the {@link Configurable} on which this method is called (not to the parents
     * or children), but when the main template is processed, the auto-includes are collected from all the
     * {@link Configurable} levels, in parent-to-child order: {@link Configuration}, {@link Template} (the main
     * template), {@link Environment}.
     * 
     * <p>
     * If there are also auto-imports ({@link #addAutoImport(String, String)}), those imports will be executed before
     * the auto-includes, hence the namespace variables are accessible for the auto-included templates.
     * 
     * <p>
     * Calling {@link #addAutoInclude(String)} with an already added template name will just move that to the end of the
     * auto-include list (within the same {@link Configurable} level). This works even if the same template name appears
     * on different {@link Configurable} levels, in which case only the inclusion on the lowest (child) level will be
     * executed.
     * 
     * @see #setAutoIncludes(List)
     */
    public void addAutoInclude(String templateName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (autoIncludes == null) {
                initAutoIncludesList();
            } else {
                autoIncludes.remove(templateName);
            }
            autoIncludes.add(templateName);
        }
    }

    private void initAutoIncludesList() {
        autoIncludes = new ArrayList<>(4);
    }
    
    /**
     * Removes all auto-includes, then calls {@link #addAutoInclude(String)} for each {@link List} items.
     * 
     * <p>Before {@linkplain Configuration#Configuration(Version) incompatible improvements} 2.3.25 it doesn't filter
     * out duplicates from the list if this method was called on a {@link Configuration} instance.
     */
    public void setAutoIncludes(List templateNames) {
        _NullArgumentException.check("templateNames", templateNames);
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (autoIncludes != null) {
                autoIncludes.clear();
            }
            for (Object templateName : templateNames) {
                if (!(templateName instanceof String)) {
                    throw new IllegalArgumentException("List items must be String-s.");
                }
                addAutoInclude((String) templateName);
            }
        }
    }
    
    /**
     * Getter pair of {@link #setAutoIncludes(List)}; do not modify the returned {@link List}! To be consistent with
     * other setting getters, if this setting was set directly on this {@link Configurable} object, this simply returns
     * that value, otherwise it returns the value from the parent {@link Configurable}. So beware, the returned value
     * doesn't reflect the {@link List} concatenation logic that FreeMarker actually uses for this setting. The returned
     * value is not the same {@link List} object that was set with {@link #setAutoIncludes(List)}, only its content is
     * the same (except that duplicate are removed). The returned value isn't a snapshot; it may or may not shows the
     * changes later made to this setting on this {@link Configurable} level (but usually it's well defined if until
     * what point settings are possibly modified).
     * 
     * <p>
     * The return value is never {@code null}; called on the {@link Configuration} (top) level, it defaults to an empty
     * {@link List}.
     * 
     * @see #getAutoIncludesWithoutFallback()
     * 
     * @since 2.3.25
     */
    public List<String> getAutoIncludes() {
        return autoIncludes != null ? autoIncludes : parent.getAutoIncludes();
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     * 
     * @since 2.3.25
     */
    public boolean isAutoIncludesSet() {
        return autoIncludes != null;
    }
    
    /**
     * Like {@link #getAutoIncludes()}, but doesn't fall back to the parent {@link Configurable} (and so it can be
     * {@code null}).
     *  
     * @since 2.3.25
     */
    public List<String> getAutoIncludesWithoutFallback() {
        return autoIncludes;
    }
    
    /**
     * Removes the auto-include from this {@link Configurable} level (not from the parents or children); see
     * {@link #addAutoInclude(String)}. Does nothing if the template is not there.
     */
    public void removeAutoInclude(String templateName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (autoIncludes != null) {
                autoIncludes.remove(templateName);
            }
        }
    }
    
    private static final String ALLOWED_CLASSES = "allowed_classes";
    private static final String TRUSTED_TEMPLATES = "trusted_templates";
    
    /**
     * Sets a FreeMarker setting by a name and string value. If you can configure FreeMarker directly with Java (or
     * other programming language), you should use the dedicated setter methods instead (like
     * {@link #setObjectWrapper(ObjectWrapper)}. This meant to be used only when you get settings from somewhere
     * as {@link String}-{@link String} name-value pairs (typically, as a {@link Properties} object). Below you find an
     * overview of the settings available.
     * 
     * <p>Note: As of FreeMarker 2.3.23, setting names can be written in camel case too. For example, instead of
     * {@code date_format} you can also use {@code dateFormat}. It's likely that camel case will become to the
     * recommended convention in the future.
     * 
     * <p>The list of settings commonly supported in all {@link Configurable} subclasses:
     * <ul>
     *   <li><p>{@code "locale"}:
     *       See {@link #setLocale(Locale)}.
     *       <br>String value: local codes with the usual format in Java, such as {@code "en_US"}.
     *
     *   <li><p>{@code "custom_number_formats"}: See {@link #setCustomNumberFormats(Map)}.
     *   <br>String value: Interpreted as an <a href="#fm_obe">object builder expression</a>.
     *   <br>Example: <code>{ "hex": com.example.HexTemplateNumberFormatFactory,
     *   "gps": com.example.GPSTemplateNumberFormatFactory }</code>
     *
     *   <li><p>{@code "custom_date_formats"}: See {@link #setCustomDateFormats(Map)}.
     *   <br>String value: Interpreted as an <a href="#fm_obe">object builder expression</a>.
     *   <br>Example: <code>{ "trade": com.example.TradeTemplateDateFormatFactory,
     *   "log": com.example.LogTemplateDateFormatFactory }</code>
     *       
     *   <li><p>{@code "template_exception_handler"}:
     *       See {@link #setTemplateExceptionHandler(TemplateExceptionHandler)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot, then it must be one of these predefined values (case insensitive):
     *       {@code "rethrow"} (means {@link TemplateExceptionHandler#RETHROW_HANDLER}),
     *       {@code "debug"} (means {@link TemplateExceptionHandler#DEBUG_HANDLER}),
     *       {@code "html_debug"} (means {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}),
     *       {@code "ignore"} (means {@link TemplateExceptionHandler#IGNORE_HANDLER}),
     *       {@code "default"} (only allowed for {@link Configuration} instances) for the default.
     *       
     *   <li><p>{@code "arithmetic_engine"}:
     *       See {@link #setArithmeticEngine(ArithmeticEngine)}.  
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot,
     *       then it must be one of these special values (case insensitive):
     *       {@code "bigdecimal"}, {@code "conservative"}.
     *       
     *   <li><p>{@code "object_wrapper"}:
     *       See {@link #setObjectWrapper(ObjectWrapper)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>, with the addition that {@link DefaultObjectWrapper}, {@link DefaultObjectWrapper} and
     *       {@link RestrictedObjectWrapper} can be referred without package name. For example, these strings are valid
     *       values: {@code "DefaultObjectWrapper(3.0.0)"},
     *       {@code "DefaultObjectWrapper(2.3.21, simpleMapWrapper=true)"}.
     *       <br>If the value does not contain dot, then it must be one of these special values (case insensitive):
     *       {@code "default"} means the default of {@link Configuration},
     *       {@code "restricted"} means the a {@link RestrictedObjectWrapper} instance.
     *
     *   <li><p>{@code "number_format"}: See {@link #setNumberFormat(String)}.
     *   
     *   <li><p>{@code "boolean_format"}: See {@link #setBooleanFormat(String)} .
     *   
     *   <li><p>{@code "date_format", "time_format", "datetime_format"}:
     *       See {@link #setDateFormat(String)}, {@link #setTimeFormat(String)}, {@link #setDateTimeFormat(String)}. 
     *        
     *   <li><p>{@code "time_zone"}:
     *       See {@link #setTimeZone(TimeZone)}.
     *       <br>String value: With the format as {@link TimeZone#getTimeZone} defines it. Also, since 2.3.21
     *       {@code "JVM default"} can be used that will be replaced with the actual JVM default time zone when
     *       {@link #setSetting(String, String)} is called.
     *       For example {@code "GMT-8:00"} or {@code "America/Los_Angeles"}
     *       <br>If you set this setting, consider setting {@code sql_date_and_time_time_zone}
     *       too (see below)! 
     *       
     *   <li><p>{@code sql_date_and_time_time_zone}:
     *       See {@link #setSQLDateAndTimeTimeZone(TimeZone)}.
     *       Since 2.3.21.
     *       <br>String value: With the format as {@link TimeZone#getTimeZone} defines it. Also, {@code "JVM default"}
     *       can be used that will be replaced with the actual JVM default time zone when
     *       {@link #setSetting(String, String)} is called. Also {@code "null"} can be used, which has the same effect
     *       as {@link #setSQLDateAndTimeTimeZone(TimeZone) setSQLDateAndTimeTimeZone(null)}.
     *       
     *   <li><p>{@code "output_encoding"}:
     *       See {@link #setOutputEncoding(String)}.
     *       
     *   <li><p>{@code "url_escaping_charset"}:
     *       See {@link #setURLEscapingCharset(String)}.
     *       
     *   <li><p>{@code "auto_flush"}:
     *       See {@link #setAutoFlush(boolean)}.
     *       Since 2.3.17.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     *   <li><p>{@code "auto_import"}:
     *       See {@link Configuration#setAutoImports(Map)}
     *       <br>String value is something like:
     *       <br>{@code /lib/form.ftl as f, /lib/widget as w, "/lib/odd name.ftl" as odd}
     *       
     *   <li><p>{@code "auto_include"}: Sets the list of auto-includes.
     *       See {@link Configuration#setAutoIncludes(List)}
     *       <br>String value is something like:
     *       <br>{@code /include/common.ftl, "/include/evil name.ftl"}
     *       
     *   <li><p>{@code "lazy_auto_imports"}:
     *       See {@link Configuration#setLazyAutoImports(Boolean)}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}), case insensitive. Also can be {@code "null"}.

     *   <li><p>{@code "lazy_imports"}:
     *       See {@link Configuration#setLazyImports(boolean)}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}), case insensitive.
     *       
     *   <li><p>{@code "new_builtin_class_resolver"}:
     *       See {@link #setNewBuiltinClassResolver(TemplateClassResolver)}.
     *       Since 2.3.17.
     *       The value must be one of these (ignore the quotation marks):
     *       <ol>
     *         <li><p>{@code "unrestricted"}:
     *             Use {@link TemplateClassResolver#UNRESTRICTED_RESOLVER}
     *         <li><p>{@code "allows_nothing"}:
     *             Use {@link TemplateClassResolver#ALLOWS_NOTHING_RESOLVER}
     *         <li><p>Something that contains colon will use
     *             {@link OptInTemplateClassResolver} and is expected to
     *             store comma separated values (possibly quoted) segmented
     *             with {@code "allowed_classes:"} and/or
     *             {@code "trusted_templates:"}. Examples of valid values:
     *             
     *             <table style="width: auto; border-collapse: collapse" border="1"
     *                  summary="trusted_template value examples">
     *               <tr>
     *                 <th>Setting value
     *                 <th>Meaning
     *               <tr>
     *                 <td>
     *                   {@code allowed_classes: com.example.C1, com.example.C2,
     *                   trusted_templates: lib/*, safe.ftl}                 
     *                 <td>
     *                   Only allow instantiating the {@code com.example.C1} and
     *                   {@code com.example.C2} classes. But, allow templates
     *                   within the {@code lib/} directory (like
     *                   {@code lib/foo/bar.ftl}) and template {@code safe.ftl}
     *                   (that does not match {@code foo/safe.ftl}, only
     *                   exactly {@code safe.ftl}) to instantiate anything
     *                   that {@link TemplateClassResolver#UNRESTRICTED_RESOLVER} allows.
     *               <tr>
     *                 <td>
     *                   {@code allowed_classes: com.example.C1, com.example.C2}
     *                 <td>Only allow instantiating the {@code com.example.C1} and
     *                   {@code com.example.C2} classes. There are no
     *                   trusted templates.
     *               <tr>
     *                 <td>
                         {@code trusted_templates: lib/*, safe.ftl}                 
     *                 <td>
     *                   Do not allow instantiating any classes, except in
     *                   templates inside {@code lib/} or in template 
     *                   {@code safe.ftl}.
     *             </table>
     *             
     *             <p>For more details see {@link OptInTemplateClassResolver}.
     *             
     *         <li><p>Otherwise if the value contains dot, it's interpreted as an <a href="#fm_obe">object builder
     *             expression</a>.
     *       </ol>
     *       Note that the {@code safer} option was removed in FreeMarker 3.0.0, as it has become equivalent with
     *       {@code "unrestricted"}, as the classes it has blocked were removed from FreeMarker.
     *   <li><p>{@code "show_error_tips"}:
     *       See {@link #setShowErrorTips(boolean)}.
     *       Since 2.3.21.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     *   <li><p>{@code api_builtin_enabled}:
     *       See {@link #setAPIBuiltinEnabled(boolean)}.
     *       Since 2.3.22.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     * </ul>
     * 
     * <p>{@link Configuration} (a subclass of {@link Configurable}) also understands these:</p>
     * <ul>
     *   <li><p>{@code "auto_escaping"}:
     *       See {@link Configuration#setAutoEscapingPolicy(int)}
     *       <br>String value: {@code "enable_if_default"} or {@code "enableIfDefault"} for
     *       {@link Configuration#ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY},
     *       {@code "enable_if_supported"} or {@code "enableIfSupported"} for
     *       {@link Configuration#ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY}
     *       {@code "disable"} for {@link Configuration#DISABLE_AUTO_ESCAPING_POLICY}.
     *       
     *   <li><p>{@code "default_encoding"}:
     *       See {@link Configuration#setDefaultEncoding(String)}.
     *       <br>As the default value is the system default, which can change
     *       from one server to another, <b>you should always set this!</b>
     *       
     *   <li><p>{@code "localized_lookup"}:
     *       See {@link Configuration#setLocalizedLookup}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}).
     *       ASTDirCase insensitive.
     *       
     *   <li><p>{@code "output_format"}:
     *       See {@link Configuration#setOutputFormat(OutputFormat)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or an
     *       <a href="#fm_obe">object builder expression</a> that gives an {@link OutputFormat}, for example
     *       {@code HTMLOutputFormat} or {@code XMLOutputFormat}.
     *       
     *   <li><p>{@code "registered_custom_output_formats"}:
     *       See {@link Configuration#setRegisteredCustomOutputFormats(Collection)}.
     *       <br>String value: an <a href="#fm_obe">object builder expression</a> that gives a {@link List} of
     *       {@link OutputFormat}-s.
     *       Example: {@code [com.example.MyOutputFormat(), com.example.MyOtherOutputFormat()]}
     *       
     *   <li><p>{@code "whitespace_stripping"}:
     *       See {@link Configuration#setWhitespaceStripping}.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code yes}, etc.
     *       
     *   <li><p>{@code "cache_storage"}:
     *       See {@link Configuration#setCacheStorage}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot,
     *       then a {@link org.apache.freemarker.core.templateresolver.impl.MruCacheStorage} will be used with the
     *       maximum strong and soft sizes specified with the setting value. Examples
     *       of valid setting values:
     *       
     *       <table style="width: auto; border-collapse: collapse" border="1" summary="cache_storage value examples">
     *         <tr><th>Setting value<th>max. strong size<th>max. soft size
     *         <tr><td>{@code "strong:50, soft:500"}<td>50<td>500
     *         <tr><td>{@code "strong:100, soft"}<td>100<td>{@code Integer.MAX_VALUE}
     *         <tr><td>{@code "strong:100"}<td>100<td>0
     *         <tr><td>{@code "soft:100"}<td>0<td>100
     *         <tr><td>{@code "strong"}<td>{@code Integer.MAX_VALUE}<td>0
     *         <tr><td>{@code "soft"}<td>0<td>{@code Integer.MAX_VALUE}
     *       </table>
     *       
     *       <p>The value is not case sensitive. The order of <tt>soft</tt> and <tt>strong</tt>
     *       entries is not significant.
     *       
     *   <li><p>{@code "template_update_delay"}:
     *       Template update delay in <b>seconds</b> (not in milliseconds) if no unit is specified; see
     *       {@link Configuration#setTemplateUpdateDelayMilliseconds(long)} for more.
     *       <br>String value: Valid positive integer, optionally followed by a time unit (recommended). The default
     *       unit is seconds. It's strongly recommended to specify the unit for clarity, like in "500 ms" or "30 s".
     *       Supported units are: "s" (seconds), "ms" (milliseconds), "m" (minutes), "h" (hours). The whitespace between
     *       the unit and the number is optional. Units are only supported since 2.3.23.
     *       
     *   <li><p>{@code "tag_syntax"}:
     *       See {@link Configuration#setTagSyntax(int)}.
     *       <br>String value: Must be one of
     *       {@code "auto_detect"}, {@code "angle_bracket"}, and {@code "square_bracket"}. 
     *       
     *   <li><p>{@code "naming_convention"}:
     *       See {@link Configuration#setNamingConvention(int)}.
     *       <br>String value: Must be one of
     *       {@code "auto_detect"}, {@code "legacy"}, and {@code "camel_case"}.
     *       
     *   <li><p>{@code "incompatible_improvements"}:
     *       See {@link Configuration#setIncompatibleImprovements(Version)}.
     *       <br>String value: version number like {@code 2.3.20}.
     *       
     *   <li><p>{@code "recognize_standard_file_extensions"}:
     *       See {@link Configuration#setRecognizeStandardFileExtensions(boolean)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or {@code "true"}, {@code "false"},
     *       {@code yes}, etc.
     *       
     *   <li><p>{@code "template_configurations"}:
     *       See: {@link Configuration#setTemplateConfigurations(org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory)}.
     *       <br>String value: Interpreted as an <a href="#fm_obe">object builder expression</a>,
     *       can be {@code null}.
     *       
     *   <li><p>{@code "template_loader"}:
     *       See: {@link Configuration#setTemplateLoader(TemplateLoader)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or else interpreted as an
     *       <a href="#fm_obe">object builder expression</a>. {@code "null"} is also allowed.
     *       
     *   <li><p>{@code "template_lookup_strategy"}:
     *       See: {@link Configuration#setTemplateLookupStrategy(org.apache.freemarker.core.templateresolver.TemplateLookupStrategy)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or else interpreted as an
     *       <a href="#fm_obe">object builder expression</a>.
     *       
     *   <li><p>{@code "template_name_format"}:
     *       See: {@link Configuration#setTemplateNameFormat(org.apache.freemarker.core.templateresolver.TemplateNameFormat)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, {@code "default_2_3_0"}
     *       for {@link DefaultTemplateNameFormatFM2#INSTANCE}, {@code "default_2_4_0"} for
     *       {@link DefaultTemplateNameFormat#INSTANCE}.
     * </ul>
     * 
     * <p><a name="fm_obe"></a>Regarding <em>object builder expressions</em> (used by the setting values where it was
     * indicated):
     * <ul>
     *   <li><p>Before FreeMarker 2.3.21 it had to be a fully qualified class name, and nothing else.</li>
     *   <li><p>Since 2.3.21, the generic syntax is:
     *       <tt><i>className</i>(<i>constrArg1</i>, <i>constrArg2</i>, ... <i>constrArgN</i>,
     *       <i>propName1</i>=<i>propValue1</i>, <i>propName2</i>=<i>propValue2</i>, ...
     *       <i>propNameN</i>=<i>propValueN</i>)</tt>,
     *       where
     *       <tt><i>className</i></tt> is the fully qualified class name of the instance to create (except if we have
     *       builder class or <tt>INSTANCE</tt> field around, but see that later),
     *       <tt><i>constrArg</i></tt>-s are the values of constructor arguments,
     *       and <tt><i>propName</i>=<i>propValue</i></tt>-s set JavaBean properties (like <tt>x=1</tt> means
     *       <tt>setX(1)</tt>) on the created instance. You can have any number of constructor arguments and property
     *       setters, including 0. Constructor arguments must precede any property setters.   
     *   </li>
     *   <li>
     *     Example: <tt>com.example.MyObjectWrapper(1, 2, exposeFields=true, cacheSize=5000)</tt> is nearly
     *     equivalent with this Java code:
     *     <tt>obj = new com.example.MyObjectWrapper(1, 2); obj.setExposeFields(true); obj.setCacheSize(5000);</tt>
     *   </li>
     *   <li>
     *      <p>If you have no constructor arguments and property setters, and the <tt><i>className</i></tt> class has
     *      a public static {@code INSTANCE} field, the value of that filed will be the value of the expression, and
     *      the constructor won't be called.
     *   </li>
     *   <li>
     *      <p>If there exists a class named <tt><i>className</i>Builder</tt>, then that class will be instantiated
     *      instead with the given constructor arguments, and the JavaBean properties of that builder instance will be
     *      set. After that, the public <tt>build()</tt> method of the instance will be called, whose return value
     *      will be the value of the whole expression. (The builder class and the <tt>build()</tt> method is simply
     *      found by name, there's no special interface to implement.)Note that if you have a builder class, you don't
     *      actually need a <tt><i>className</i></tt> class (since 2.3.24); after all,
     *      <tt><i>className</i>Builder.build()</tt> can return any kind of object. 
     *   </li>
     *   <li>
     *      <p>Currently, the values of arguments and properties can only be one of these:
     *      <ul>
     *        <li>A numerical literal, like {@code 123} or {@code -1.5}. The value will be automatically converted to
     *        the type of the target (just like in FTL). However, a target type is only available if the number will
     *        be a parameter to a method or constructor, not when it's a value (or key) in a {@code List} or
     *        {@code Map} literal. Thus in the last case the type of number will be like in Java language, like
     *        {@code 1} is an {@code int}, and {@code 1.0} is a {@code double}, and {@code 1.0f} is a {@code float},
     *        etc. In all cases, the standard Java type postfixes can be used ("f", "d", "l"), plus "bd" for
     *        {@code BigDecimal} and "bi" for {@code BigInteger}.</li>
     *        <li>A boolean literal: {@code true} or {@code false}
     *        <li>The null literal: {@code null}
     *        <li>A string literal with FTL syntax, except that  it can't contain <tt>${...}</tt>-s and
     *            <tt>#{...}</tt>-s. Examples: {@code "Line 1\nLine 2"} or {@code r"C:\temp"}.
     *        <li>A list literal (since 2.3.24) with FTL-like syntax, for example {@code [ 'foo', 2, true ]}.
     *            If the parameter is expected to be array, the list will be automatically converted to array.
     *            The list items can be any kind of expression, like even object builder expressions.
     *        <li>A map literal (since 2.3.24) with FTL-like syntax, for example <code>{ 'foo': 2, 'bar': true }</code>.
     *            The keys and values can be any kind of expression, like even object builder expressions.
     *            The resulting Java object will be a {@link Map} that keeps the item order ({@link LinkedHashMap} as
     *            of this writing).
     *        <li>A reference to a public static filed, like {@code Configuration.AUTO_DETECT_TAG_SYNTAX} or
     *            {@code com.example.MyClass.MY_CONSTANT}.
     *        <li>An object builder expression. That is, object builder expressions can be nested into each other. 
     *      </ul>
     *   </li>
     *   <li>
     *     The same kind of expression as for parameters can also be used as top-level expressions (though it's
     *     rarely useful, apart from using {@code null}).
     *   </li>
     *   <li>
     *     <p>The top-level object builder expressions may omit {@code ()}.
     *   </li>
     *   <li>
     *     <p>The following classes can be referred to with simple (unqualified) name instead of fully qualified name:
     *     {@link DefaultObjectWrapper}, {@link DefaultObjectWrapper}, {@link RestrictedObjectWrapper}, {@link Locale},
     *     {@link TemplateConfiguration}, {@link PathGlobMatcher}, {@link FileNameGlobMatcher}, {@link PathRegexMatcher},
     *     {@link AndMatcher}, {@link OrMatcher}, {@link NotMatcher}, {@link ConditionalTemplateConfigurationFactory},
     *     {@link MergingTemplateConfigurationFactory}, {@link FirstMatchTemplateConfigurationFactory},
     *     {@link HTMLOutputFormat}, {@link XMLOutputFormat}, {@link RTFOutputFormat}, {@link PlainTextOutputFormat},
     *     {@link UndefinedOutputFormat}, {@link Configuration}.
     *   </li>
     *   <li>
     *     <p>{@link TimeZone} objects can be created like {@code TimeZone("UTC")}, despite that there's no a such
     *     constructor (since 2.3.24).
     *   </li>
     *   <li>
     *     <p>The classes and methods that the expression meant to access must be all public.
     *   </li>
     * </ul>
     * 
     * @param name the name of the setting.
     * @param value the string that describes the new value of the setting.
     * 
     * @throws UnknownConfigurationSettingException if the name is wrong.
     * @throws ConfigurationSettingValueStringException if the new value of the setting can't be set for any other reasons.
     */
    public void setSetting(String name, String value) throws ConfigurationException {
        boolean unknown = false;
        try {
            if (LOCALE_KEY.equals(name)) {
                setLocale(_StringUtil.deduceLocale(value));
            } else if (NUMBER_FORMAT_KEY_SNAKE_CASE.equals(name) || NUMBER_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setNumberFormat(value);
            } else if (CUSTOM_NUMBER_FORMATS_KEY_SNAKE_CASE.equals(name)
                    || CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE.equals(name)) {
                Map map = (Map) _ObjectBuilderSettingEvaluator.eval(
                                value, Map.class, false, _SettingEvaluationEnvironment.getCurrent());
                checkSettingValueItemsType("Map keys", String.class, map.keySet());
                checkSettingValueItemsType("Map values", TemplateNumberFormatFactory.class, map.values());
                setCustomNumberFormats(map);
            } else if (TIME_FORMAT_KEY_SNAKE_CASE.equals(name) || TIME_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setTimeFormat(value);
            } else if (DATE_FORMAT_KEY_SNAKE_CASE.equals(name) || DATE_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setDateFormat(value);
            } else if (DATETIME_FORMAT_KEY_SNAKE_CASE.equals(name) || DATETIME_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setDateTimeFormat(value);
            } else if (CUSTOM_DATE_FORMATS_KEY_SNAKE_CASE.equals(name)
                    || CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE.equals(name)) {
                Map map = (Map) _ObjectBuilderSettingEvaluator.eval(
                                value, Map.class, false, _SettingEvaluationEnvironment.getCurrent());
                checkSettingValueItemsType("Map keys", String.class, map.keySet());
                checkSettingValueItemsType("Map values", TemplateDateFormatFactory.class, map.values());
                setCustomDateFormats(map);
            } else if (TIME_ZONE_KEY_SNAKE_CASE.equals(name) || TIME_ZONE_KEY_CAMEL_CASE.equals(name)) {
                setTimeZone(parseTimeZoneSettingValue(value));
            } else if (SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE.equals(name)
                    || SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE.equals(name)) {
                setSQLDateAndTimeTimeZone(value.equals("null") ? null : parseTimeZoneSettingValue(value));
            } else if (TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE.equals(name)
                    || TEMPLATE_EXCEPTION_HANDLER_KEY_CAMEL_CASE.equals(name)) {
                if (value.indexOf('.') == -1) {
                    if ("debug".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.DEBUG_HANDLER);
                    } else if ("html_debug".equalsIgnoreCase(value) || "htmlDebug".equals(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                    } else if ("ignore".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.IGNORE_HANDLER);
                    } else if ("rethrow".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.RETHROW_HANDLER);
                    } else if (DEFAULT.equalsIgnoreCase(value) && this instanceof Configuration) {
                        ((Configuration) this).unsetTemplateExceptionHandler();
                    } else {
                        throw invalidSettingValueException(name, value);
                    }
                } else {
                    setTemplateExceptionHandler((TemplateExceptionHandler) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateExceptionHandler.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (ARITHMETIC_ENGINE_KEY_SNAKE_CASE.equals(name) || ARITHMETIC_ENGINE_KEY_CAMEL_CASE.equals(name)) {
                if (value.indexOf('.') == -1) { 
                    if ("bigdecimal".equalsIgnoreCase(value)) {
                        setArithmeticEngine(BigDecimalArithmeticEngine.INSTANCE);
                    } else if ("conservative".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ConservativeArithmeticEngine.INSTANCE);
                    } else {
                        throw invalidSettingValueException(name, value);
                    }
                } else {
                    setArithmeticEngine((ArithmeticEngine) _ObjectBuilderSettingEvaluator.eval(
                            value, ArithmeticEngine.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (OBJECT_WRAPPER_KEY_SNAKE_CASE.equals(name) || OBJECT_WRAPPER_KEY_CAMEL_CASE.equals(name)) {
                if (DEFAULT.equalsIgnoreCase(value)) {
                    if (this instanceof Configuration) {
                        ((Configuration) this).unsetObjectWrapper();
                    } else {
                        setObjectWrapper(Configuration.getDefaultObjectWrapper(Configuration.VERSION_3_0_0));
                    }
                } else if ("restricted".equalsIgnoreCase(value)) {
                    setObjectWrapper(new RestrictedObjectWrapper(Configuration.VERSION_3_0_0));
                } else {
                    setObjectWrapper((ObjectWrapper) _ObjectBuilderSettingEvaluator.eval(
                                    value, ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (BOOLEAN_FORMAT_KEY_SNAKE_CASE.equals(name) || BOOLEAN_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setBooleanFormat(value);
            } else if (OUTPUT_ENCODING_KEY_SNAKE_CASE.equals(name) || OUTPUT_ENCODING_KEY_CAMEL_CASE.equals(name)) {
                setOutputEncoding(value);
            } else if (URL_ESCAPING_CHARSET_KEY_SNAKE_CASE.equals(name)
                    || URL_ESCAPING_CHARSET_KEY_CAMEL_CASE.equals(name)) {
                setURLEscapingCharset(value);
            } else if (AUTO_FLUSH_KEY_SNAKE_CASE.equals(name) || AUTO_FLUSH_KEY_CAMEL_CASE.equals(name)) {
                setAutoFlush(_StringUtil.getYesNo(value));
            } else if (SHOW_ERROR_TIPS_KEY_SNAKE_CASE.equals(name) || SHOW_ERROR_TIPS_KEY_CAMEL_CASE.equals(name)) {
                setShowErrorTips(_StringUtil.getYesNo(value));
            } else if (API_BUILTIN_ENABLED_KEY_SNAKE_CASE.equals(name)
                    || API_BUILTIN_ENABLED_KEY_CAMEL_CASE.equals(name)) {
                setAPIBuiltinEnabled(_StringUtil.getYesNo(value));
            } else if (NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE.equals(name)
                    || NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE.equals(name)) {
                if ("unrestricted".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.UNRESTRICTED_RESOLVER);
                } else if ("allows_nothing".equals(value) || "allowsNothing".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
                } else if (value.indexOf(":") != -1) {
                    List<KeyValuePair<List<String>>> segments = parseAsSegmentedList(value);
                    Set allowedClasses = null;
                    List<String> trustedTemplates = null;
                    for (KeyValuePair<List<String>> segment : segments) {
                        String segmentKey = segment.getKey();
                        List<String> segmentValue = segment.getValue();
                        if (segmentKey.equals(ALLOWED_CLASSES)) {
                            allowedClasses = new HashSet(segmentValue);
                        } else if (segmentKey.equals(TRUSTED_TEMPLATES)) {
                            trustedTemplates = segmentValue;
                        } else {
                            throw new GenericParseException(
                                    "Unrecognized list segment key: " + _StringUtil.jQuote(segmentKey) +
                                            ". Supported keys are: \"" + ALLOWED_CLASSES + "\", \"" +
                                            TRUSTED_TEMPLATES + "\"");
                        }
                    }
                    setNewBuiltinClassResolver(
                            new OptInTemplateClassResolver(allowedClasses, trustedTemplates));
                } else if (value.indexOf('.') != -1) {
                    setNewBuiltinClassResolver((TemplateClassResolver) _ObjectBuilderSettingEvaluator.eval(
                                    value, TemplateClassResolver.class, false,
                                    _SettingEvaluationEnvironment.getCurrent()));
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE.equals(name)
                    || LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE.equals(name)) {
                setLogTemplateExceptions(_StringUtil.getYesNo(value));
            } else if (LAZY_AUTO_IMPORTS_KEY_SNAKE_CASE.equals(name) || LAZY_AUTO_IMPORTS_KEY_CAMEL_CASE.equals(name)) {
                setLazyAutoImports(value.equals(NULL) ? null : Boolean.valueOf(_StringUtil.getYesNo(value)));
            } else if (LAZY_IMPORTS_KEY_SNAKE_CASE.equals(name) || LAZY_IMPORTS_KEY_CAMEL_CASE.equals(name)) {
                setLazyImports(_StringUtil.getYesNo(value));
            } else if (AUTO_INCLUDE_KEY_SNAKE_CASE.equals(name)
                    || AUTO_INCLUDE_KEY_CAMEL_CASE.equals(name)) {
                setAutoIncludes(parseAsList(value));
            } else if (AUTO_IMPORT_KEY_SNAKE_CASE.equals(name) || AUTO_IMPORT_KEY_CAMEL_CASE.equals(name)) {
                setAutoImports(parseAsImportList(value));
            } else {
                unknown = true;
            }
        } catch (Exception e) {
            throw settingValueAssignmentException(name, value, e);
        }
        if (unknown) {
            throw unknownSettingException(name);
        }
    }

    /**
     * @throws IllegalArgumentException
     *             if the type of the some of the values isn't as expected
     */
    private void checkSettingValueItemsType(String somethingsSentenceStart, Class<?> expectedClass,
                                                  Collection<?> values) {
        if (values == null) return;
        for (Object value : values) {
            if (!expectedClass.isInstance(value)) {
                throw new IllegalArgumentException(somethingsSentenceStart + " must be instances of "
                        + _ClassUtil.getShortClassName(expectedClass) + ", but one of them was a(n) "
                        + _ClassUtil.getShortClassNameOfObject(value) + ".");
            }
        }
    }

    /**
     * Returns the valid setting names that aren't {@link Configuration}-only.
     *
     * @param camelCase
     *            If we want the setting names with camel case naming convention, or with snake case (legacy) naming
     *            convention.
     * 
     * @see Configuration#getSettingNames(boolean)
     * 
     * @since 2.3.24
     */
    public Set<String> getSettingNames(boolean camelCase) {
        return new _SortedArraySet<>(camelCase ? SETTING_NAMES_CAMEL_CASE : SETTING_NAMES_SNAKE_CASE);
    }

    private TimeZone parseTimeZoneSettingValue(String value) {
        TimeZone tz;
        if (JVM_DEFAULT.equalsIgnoreCase(value)) {
            tz = TimeZone.getDefault();
        } else {
            tz = TimeZone.getTimeZone(value);
        }
        return tz;
    }
    
    protected Environment getEnvironment() {
        return this instanceof Environment
            ? (Environment) this
            : Environment.getCurrentEnvironment();
    }
    
    /**
     * Creates the exception that should be thrown when a setting name isn't recognized.
     */
    protected final UnknownConfigurationSettingException unknownSettingException(String name) {
        Version removalVersion = getRemovalVersionForUnknownSetting(name);
        return removalVersion != null
                ? new UnknownConfigurationSettingException(name, removalVersion)
                : new UnknownConfigurationSettingException(name, getCorrectedNameForUnknownSetting(name));
    }

    /**
     * If a setting name is unknown because it was removed over time, then returns the version where it was removed,
     * otherwise returns {@code null}.
     */
    protected Version getRemovalVersionForUnknownSetting(String name) {
        if (name.equals("classic_compatible") || name.equals("classicCompatible")) {
            return Configuration.VERSION_3_0_0;
        }
        return null;
    }
    
    /**
     * @param name The wrong name
     * @return The corrected name, or {@code null} if there's no known correction
     * @since 2.3.21
     */
    protected String getCorrectedNameForUnknownSetting(String name) {
        return null;
    }
    
    protected final ConfigurationSettingValueStringException settingValueAssignmentException(
            String name, String value, Throwable cause) {
        return new ConfigurationSettingValueStringException(name, value, cause);
    }

    protected final TemplateException invalidSettingValueException(String name, String value) {
        return invalidSettingValueException(name, value, null);
    }
    
    protected final TemplateException invalidSettingValueException(String name, String value, String reason) {
        return new _MiscTemplateException(getEnvironment(),
                "Invalid value for setting ", new _DelayedJQuote(name), ": ", new _DelayedJQuote(value),
                (reason != null ? ": " : null), (reason != null ? reason : null));
    }

    /**
     * Set the settings stored in a <code>Properties</code> object.
     * 
     * @throws ConfigurationException if the <code>Properties</code> object contains
     *     invalid keys, or invalid setting values, or any other error occurs
     *     while changing the settings.
     */    
    public void setSettings(Properties props) throws ConfigurationException {
        final _SettingEvaluationEnvironment prevEnv = _SettingEvaluationEnvironment.startScope();
        try {
            for (String key : props.stringPropertyNames()) {
                setSetting(key, props.getProperty(key).trim());
            }
        } finally {
            _SettingEvaluationEnvironment.endScope(prevEnv);
        }
    }

    /**
     * Internal entry point for setting unnamed custom attributes.
     * 
     * @see CustomAttribute
     */
    void setCustomAttribute(Object key, Object value) {
        synchronized (customAttributes) {
            customAttributes.put(key, value);
        }
    }

    /**
     * Internal entry point for getting unnamed custom attributes.
     * 
     * @see CustomAttribute
     */
    Object getCustomAttribute(Object key, CustomAttribute attr) {
        synchronized (customAttributes) {
            Object o = customAttributes.get(key);
            if (o == null && !customAttributes.containsKey(key)) {
                o = attr.create();
                customAttributes.put(key, o);
            }
            return o;
        }
    }
    
    boolean isCustomAttributeSet(Object key) {
        return customAttributes.containsKey(key);
    }
    
    /**
     * For internal usage only, copies the custom attributes set directly on this objects into another
     * {@link Configurable}. The target {@link Configurable} is assumed to be not seen be other thread than the current
     * one yet. (That is, the operation is not synchronized on the target {@link Configurable}, only on the source 
     * {@link Configurable})
     * 
     * @since 2.3.24
     */
    void copyDirectCustomAttributes(Configurable target, boolean overwriteExisting) {
        synchronized (customAttributes) {
            for (Entry<?, ?> custAttrEnt : customAttributes.entrySet()) {
                Object custAttrKey = custAttrEnt.getKey();
                if (overwriteExisting || !target.isCustomAttributeSet(custAttrKey)) {
                    if (custAttrKey instanceof String) {
                        target.setCustomAttribute((String) custAttrKey, custAttrEnt.getValue());
                    } else {
                        target.setCustomAttribute(custAttrKey, custAttrEnt.getValue());
                    }
                }
            }
        }
    }
    
    /**
     * Sets a named custom attribute for this configurable.
     *
     * @param name the name of the custom attribute
     * @param value the value of the custom attribute. You can set the value to
     * null, however note that there is a semantic difference between an
     * attribute set to null and an attribute that is not present, see
     * {@link #removeCustomAttribute(String)}.
     */
    public void setCustomAttribute(String name, Object value) {
        synchronized (customAttributes) {
            customAttributes.put(name, value);
        }
    }
    
    /**
     * Returns an array with names of all custom attributes defined directly 
     * on this configurable. (That is, it doesn't contain the names of custom attributes
     * defined indirectly on its parent configurables.) The returned array is never null,
     * but can be zero-length.
     * The order of elements in the returned array is not defined and can change
     * between invocations.  
     */
    public String[] getCustomAttributeNames() {
        synchronized (customAttributes) {
            Collection names = new LinkedList(customAttributes.keySet());
            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                if (!(iter.next() instanceof String)) {
                    iter.remove();
                }
            }
            return (String[]) names.toArray(new String[names.size()]);
        }
    }
    
    /**
     * Removes a named custom attribute for this configurable. Note that this
     * is different than setting the custom attribute value to null. If you
     * set the value to null, {@link #getCustomAttribute(String)} will return
     * null, while if you remove the attribute, it will return the value of
     * the attribute in the parent configurable (if there is a parent 
     * configurable, that is). 
     *
     * @param name the name of the custom attribute
     */
    public void removeCustomAttribute(String name) {
        synchronized (customAttributes) {
            customAttributes.remove(name);
        }
    }

    /**
     * Retrieves a named custom attribute for this configurable. If the 
     * attribute is not present in the configurable, and the configurable has
     * a parent, then the parent is looked up as well.
     *
     * @param name the name of the custom attribute
     *
     * @return the value of the custom attribute. Note that if the custom attribute
     * was created with <tt>&lt;#ftl&nbsp;attributes={...}&gt;</tt>, then this value is already
     * unwrapped (i.e. it's a <code>String</code>, or a <code>List</code>, or a
     * <code>Map</code>, ...etc., not a FreeMarker specific class).
     */
    public Object getCustomAttribute(String name) {
        Object retval;
        synchronized (customAttributes) {
            retval = customAttributes.get(name);
            if (retval == null && customAttributes.containsKey(name)) {
                return null;
            }
        }
        if (retval == null && parent != null) {
            return parent.getCustomAttribute(name);
        }
        return retval;
    }
    
    /**
     * Executes the auto-imports and auto-includes for the main template of this environment.
     * This is not meant to be called or overridden by code outside of FreeMarker. 
     */
    protected void doAutoImportsAndIncludes(Environment env)
    throws TemplateException, IOException {
        if (parent != null) parent.doAutoImportsAndIncludes(env);
    }

    protected final List<String> parseAsList(String text) throws GenericParseException {
        return new SettingStringParser(text).parseAsList();
    }

    protected final List<KeyValuePair<List<String>>> parseAsSegmentedList(String text)
    throws GenericParseException {
        return new SettingStringParser(text).parseAsSegmentedList();
    }
    
    protected final HashMap parseAsImportList(String text) throws GenericParseException {
        return new SettingStringParser(text).parseAsImportList();
    }
    
    private static class KeyValuePair<V> {
        private final String key;
        private final V value;
        
        KeyValuePair(String key, V value) {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        V getValue() {
            return value;
        }
    }
    
    /**
     * Helper class for parsing setting values given with string.
     */
    private static class SettingStringParser {
        private String text;
        private int p;
        private int ln;

        private SettingStringParser(String text) {
            this.text = text;
            p = 0;
            ln = text.length();
        }

        List<KeyValuePair<List<String>>> parseAsSegmentedList() throws GenericParseException {
            List<KeyValuePair<List<String>>> segments = new ArrayList();
            List<String> currentSegment = null;
            
            char c;
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                String item = fetchStringValue();
                c = skipWS();
                
                if (c == ':') {
                    currentSegment = new ArrayList();
                    segments.add(new KeyValuePair<List<String>>(item, currentSegment));
                } else {
                    if (currentSegment == null) {
                        throw new GenericParseException(
                                "The very first list item must be followed by \":\" so " +
                                "it will be the key for the following sub-list.");
                    }
                    currentSegment.add(item);
                }
                
                if (c == ' ') break;
                if (c != ',' && c != ':') throw new GenericParseException(
                        "Expected \",\" or \":\" or the end of text but " +
                        "found \"" + c + "\"");
                p++;
            }
            return segments;
        }

        ArrayList parseAsList() throws GenericParseException {
            char c;
            ArrayList seq = new ArrayList();
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                seq.add(fetchStringValue());
                c = skipWS();
                if (c == ' ') break;
                if (c != ',') throw new GenericParseException(
                        "Expected \",\" or the end of text but " +
                        "found \"" + c + "\"");
                p++;
            }
            return seq;
        }

        HashMap parseAsImportList() throws GenericParseException {
            char c;
            HashMap map = new HashMap();
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                String lib = fetchStringValue();

                c = skipWS();
                if (c == ' ') throw new GenericParseException(
                        "Unexpected end of text: expected \"as\"");
                String s = fetchKeyword();
                if (!s.equalsIgnoreCase("as")) throw new GenericParseException(
                        "Expected \"as\", but found " + _StringUtil.jQuote(s));

                c = skipWS();
                if (c == ' ') throw new GenericParseException(
                        "Unexpected end of text: expected gate hash name");
                String ns = fetchStringValue();
                
                map.put(ns, lib);

                c = skipWS();
                if (c == ' ') break;
                if (c != ',') throw new GenericParseException(
                        "Expected \",\" or the end of text but "
                        + "found \"" + c + "\"");
                p++;
            }
            return map;
        }

        String fetchStringValue() throws GenericParseException {
            String w = fetchWord();
            if (w.startsWith("'") || w.startsWith("\"")) {
                w = w.substring(1, w.length() - 1);
            }
            return FTLUtil.unescapeStringLiteralPart(w);
        }

        String fetchKeyword() throws GenericParseException {
            String w = fetchWord();
            if (w.startsWith("'") || w.startsWith("\"")) {
                throw new GenericParseException(
                    "Keyword expected, but a string value found: " + w);
            }
            return w;
        }

        char skipWS() {
            char c;
            while (p < ln) {
                c = text.charAt(p);
                if (!Character.isWhitespace(c)) return c;
                p++;
            }
            return ' ';
        }

        private String fetchWord() throws GenericParseException {
            if (p == ln) throw new GenericParseException(
                    "Unexpeced end of text");

            char c = text.charAt(p);
            int b = p;
            if (c == '\'' || c == '"') {
                boolean escaped = false;
                char q = c;
                p++;
                while (p < ln) {
                    c = text.charAt(p);
                    if (!escaped) {
                        if (c == '\\') {
                            escaped = true;
                        } else if (c == q) {
                            break;
                        }
                    } else {
                        escaped = false;
                    }
                    p++;
                }
                if (p == ln) {
                    throw new GenericParseException("Missing " + q);
                }
                p++;
                return text.substring(b, p);
            } else {
                do {
                    c = text.charAt(p);
                    if (!(Character.isLetterOrDigit(c)
                            || c == '/' || c == '\\' || c == '_'
                            || c == '.' || c == '-' || c == '!'
                            || c == '*' || c == '?')) break;
                    p++;
                } while (p < ln);
                if (b == p) {
                    throw new GenericParseException("Unexpected character: " + c);
                } else {
                    return text.substring(b, p);
                }
            }
        }
    }
    
}
