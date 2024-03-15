//  Copyright (c) 2024 JC Cormier
//  All rights reserved.
//  SPDX-License-Identifier: MIT
//  For full license text, see LICENSE file in the repo root or https://opensource.org/licenses/MIT

package io.github.asyncomatic.readers;

import io.github.asyncomatic.workers.Worker;

public interface Reader {
    public void listen(Worker worker);

}
