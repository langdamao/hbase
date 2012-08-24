/*
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.filter;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hbase.DeserializationException;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.protobuf.generated.FilterProtos;

import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Simple filter that returns first N columns on row only.
 * This filter was written to test filters in Get and as soon as it gets
 * its quota of columns, {@link #filterAllRemaining()} returns true.  This
 * makes this filter unsuitable as a Scan filter.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class ColumnCountGetFilter extends FilterBase {
  private int limit = 0;
  private int count = 0;

  public ColumnCountGetFilter(final int n) {
    Preconditions.checkArgument(n >= 0, "limit be positive %s", n);
    this.limit = n;
  }

  public int getLimit() {
    return limit;
  }

  @Override
  public boolean filterAllRemaining() {
    return this.count > this.limit;
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    this.count++;
    return filterAllRemaining() ? ReturnCode.SKIP: ReturnCode.INCLUDE;
  }

  @Override
  public void reset() {
    this.count = 0;
  }

  public static Filter createFilterFromArguments(ArrayList<byte []> filterArguments) {
    Preconditions.checkArgument(filterArguments.size() == 1,
                                "Expected 1 but got: %s", filterArguments.size());
    int limit = ParseFilter.convertByteArrayToInt(filterArguments.get(0));
    return new ColumnCountGetFilter(limit);
  }

  /**
   * @return The filter serialized using pb
   */
  public byte [] toByteArray() {
    FilterProtos.ColumnCountGetFilter.Builder builder =
      FilterProtos.ColumnCountGetFilter.newBuilder();
    builder.setLimit(this.limit);
    return builder.build().toByteArray();
  }

  /**
   * @param pbBytes A pb serialized {@link ColumnCountGetFilter} instance
   * @return An instance of {@link ColumnCountGetFilter} made from <code>bytes</code>
   * @throws DeserializationException
   * @see {@link #toByteArray()}
   */
  public static ColumnCountGetFilter parseFrom(final byte [] pbBytes)
  throws DeserializationException {
    FilterProtos.ColumnCountGetFilter proto;
    try {
      proto = FilterProtos.ColumnCountGetFilter.parseFrom(pbBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new DeserializationException(e);
    }
    return new ColumnCountGetFilter(proto.getLimit());
  }

  /**
   * @param other
   * @return true if and only if the fields of the filter that are serialized
   * are equal to the corresponding fields in other.  Used for testing.
   */
  boolean areSerializedFieldsEqual(Filter o) {
    if (o == this) return true;
    if (!(o instanceof ColumnCountGetFilter)) return false;

    ColumnCountGetFilter other = (ColumnCountGetFilter)o;
    return this.getLimit() == other.getLimit();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " " + this.limit;
  }
}
