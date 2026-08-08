package com.modstack.entity;

/**
 * Implemented by MobEntityStackMixin so other mixins/classes (e.g. the
 * breeding mixin) can read/modify a mob's stack count without needing to
 * know about the mixin implementation directly.
 */
public interface StackAccess {
    int modstack$getCount();
    void modstack$setCount(int count);
}
