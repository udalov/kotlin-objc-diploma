@interface NSObject
@end

@interface A : NSObject

+ (void) voidNoArgs;
+ (void) voidWithInt: (int)intArg;
+ (void) voidWithID: (id)idArg withChar: (char)charArg;

+ (int) intNoArgs;
+ (int) intWithInt: (int)intArg;
+ (id) idWithInt: (int)intArg withChar: (char)charArg;

@end
