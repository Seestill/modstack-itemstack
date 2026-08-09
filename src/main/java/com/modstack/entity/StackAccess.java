package com.modstack.entity;

// Implemented by MobEntityStackMixin so other mixins/classes can read/modify
// a mob's stack count and exemption status without needing to know about
// the mixin implementation directly.
public interface StackAccess {
    int modstack$getCount();
    void modstack$setCount(int count);
    boolean modstack$isExempt();
}
