@protocol P
- (void)p;
@end

@protocol Q
- (void)q;
@end


@protocol Derived<P, Q>
@end
