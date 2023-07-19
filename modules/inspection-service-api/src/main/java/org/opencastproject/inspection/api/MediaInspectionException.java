/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.inspection.api;

/**
 * Exception thrown by the {@link MediaInspectionService} operations.
 */
public class MediaInspectionException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = 1336936343567817393L;

  /**
   * Creates a new inspection exception with the specified error message.
   *
   * @param message
   *          the error message
   */
  public MediaInspectionException(String message) {
    super(message);
  }

  /**
   * Creates a new inspection exception with the specified error message and wrapping the original exception.
   *
   * @param message
   *          the error message
   * @param cause
   *          the original error
   */
  public MediaInspectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new inspection exception by wrapping the original error .
   *
   * @param cause
   *          the original error
   */
  public MediaInspectionException(Throwable cause) {
    super(cause);
  }

}
