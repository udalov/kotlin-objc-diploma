#import <objc/objc.h>
#import <stddef.h>

@interface NSObject
@end

@interface A : NSObject
- (void) f_void;
- (unsigned char) f_uchar;
- (unsigned short) f_ushort;
- (unsigned int) f_uint;
- (unsigned long) f_ulong;
- (unsigned long long) f_ulonglong;
- (char) f_char;
- (BOOL) f_bool;
- (wchar_t) f_wchar_t;
- (short) f_short;
- (int) f_int;
- (long) f_long;
- (long long) f_longlong;
- (float) f_float;
- (double) f_double;
- (long double) f_longdouble;

- (id) f_id;
- (Class) f_Class;
- (SEL) f_SEL;
@end
