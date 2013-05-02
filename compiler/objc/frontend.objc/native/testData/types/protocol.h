@protocol P
@end


@interface A
- (id <P>) todo;
- (A <P>*) todo2;
@end
