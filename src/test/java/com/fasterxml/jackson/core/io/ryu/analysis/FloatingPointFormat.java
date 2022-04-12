// Copyright 2018 Ulf Adams
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.fasterxml.jackson.core.io.ryu.analysis;

enum FloatingPointFormat {
  FLOAT16(16, 5, 10),
  FLOAT32(32, 8, 23),
  FLOAT64(64, 11, 52),
  FLOAT80(80, 15, 63),
  FLOAT128(128, 15, 112),
  FLOAT256(256, 19, 236);

  private final int totalBits;
  private final int exponentBits;
  private final int mantissaBits;

  private FloatingPointFormat(int totalBits, int exponentBits, int mantissaBits) {
    this.totalBits = totalBits;
    this.exponentBits = exponentBits;
    this.mantissaBits = mantissaBits;
  }

  public int totalBits() {
    return totalBits;
  }

  public int exponentBits() {
    return exponentBits;
  }

  public int mantissaBits() {
    return mantissaBits;
  }

  public int bias() {
    return (1 << (exponentBits - 1)) - 1;
  }
}