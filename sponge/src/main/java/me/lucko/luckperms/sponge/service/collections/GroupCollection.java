/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.sponge.service.collections;

import co.aikar.timings.Timing;
import lombok.NonNull;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.groups.GroupManager;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.LuckPermsGroupSubject;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.simple.SimpleCollection;
import me.lucko.luckperms.sponge.timings.LPTiming;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupCollection implements SubjectCollection {
    private final LuckPermsService service;
    private final GroupManager manager;
    private final SimpleCollection fallback;

    public GroupCollection(LuckPermsService service, GroupManager manager) {
        this.service = service;
        this.manager = manager;
        this.fallback = new SimpleCollection(service, "fallback-groups");
    }

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_GROUP;
    }

    @Override
    public Subject get(@NonNull String id) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.GROUP_COLLECTION_GET)) {
            if (manager.isLoaded(id)) {
                return LuckPermsGroupSubject.wrapGroup(manager.get(id), service);
            }

            return fallback.get(id);
        }
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        return manager.isLoaded(id);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return manager.getAll().values().stream()
                .map(u -> LuckPermsGroupSubject.wrapGroup(u, service))
                .collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull String node) {
        return getAllWithPermission(SubjectData.GLOBAL_CONTEXT, node);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        ContextSet cs = LuckPermsService.convertContexts(contexts);
        return manager.getAll().values().stream()
                .map(u -> LuckPermsGroupSubject.wrapGroup(u, service))
                .filter(sub -> sub.getPermissionValue(cs, node) != Tristate.UNDEFINED)
                .collect(Collectors.toMap(sub -> sub, sub -> sub.getPermissionValue(cs, node).asBoolean()));
    }

    @Override
    public Subject getDefaults() {
        return service.getDefaultSubjects().get(getIdentifier());
    }
}